package com.hufsphere.linkboard.client;

import tools.jackson.databind.JsonNode;
import com.hufsphere.linkboard.client.dto.FigmaComment;
import com.hufsphere.linkboard.exception.SourceFetchFailedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class FigmaCrawlerClient {

    private static final String FIGMA_API_BASE = "https://api.figma.com";

    private final RestClient figmaRestClient;

    public List<FigmaComment> fetchComments(String accessToken, String fileKey) {
        List<JsonNode> activeComments = new ArrayList<>();
        for (JsonNode comment : fetchCommentsRaw(accessToken, fileKey).path("comments")) {
            JsonNode resolvedAt = comment.path("resolved_at");
            boolean resolved = !resolvedAt.isMissingNode() && !resolvedAt.isNull();
            if (!resolved) {
                activeComments.add(comment);
            }
        }

        Map<String, String> nodeNames = fetchNodeNames(accessToken, fileKey, extractNodeIds(activeComments));

        List<FigmaComment> comments = new ArrayList<>();
        for (JsonNode comment : activeComments) {
            String message = comment.path("message").asString("");
            if (message.isBlank()) {
                continue;
            }
            String nodeId = nodeIdOf(comment);
            String frameName = nodeId != null ? nodeNames.getOrDefault(nodeId, nodeId) : "(캔버스 코멘트)";
            String url = buildCommentUrl(fileKey, nodeId, comment.path("id").asString());
            comments.add(new FigmaComment(frameName, url, message));
        }

        return comments;
    }

    private String nodeIdOf(JsonNode comment) {
        JsonNode nodeId = comment.path("client_meta").path("node_id");
        return nodeId.isMissingNode() || nodeId.isNull() ? null : nodeId.asString();
    }

    private Set<String> extractNodeIds(List<JsonNode> comments) {
        Set<String> ids = new LinkedHashSet<>();
        for (JsonNode comment : comments) {
            String nodeId = nodeIdOf(comment);
            if (nodeId != null) {
                ids.add(nodeId);
            }
        }
        return ids;
    }

    private String buildCommentUrl(String fileKey, String nodeId, String commentId) {
        StringBuilder url = new StringBuilder("https://www.figma.com/file/").append(fileKey);
        if (nodeId != null) {
            url.append("?node-id=").append(nodeId);
        }
        return url.append("#").append(commentId).toString();
    }

    private Map<String, String> fetchNodeNames(String accessToken, String fileKey, Set<String> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Map.of();
        }

        JsonNode body = fetchNodesRaw(accessToken, fileKey, String.join(",", nodeIds));
        Map<String, String> names = new HashMap<>();
        for (String nodeId : nodeIds) {
            String name = body.path("nodes").path(nodeId).path("document").path("name").asString(nodeId);
            names.put(nodeId, name);
        }
        return names;
    }

    private JsonNode fetchCommentsRaw(String accessToken, String fileKey) {
        try {
            return figmaRestClient.get()
                    .uri(FIGMA_API_BASE + "/v1/files/{fileKey}/comments", fileKey)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new SourceFetchFailedException("Figma 코멘트 조회에 실패했습니다");
        }
    }

    private JsonNode fetchNodesRaw(String accessToken, String fileKey, String ids) {
        try {
            return figmaRestClient.get()
                    .uri(FIGMA_API_BASE + "/v1/files/{fileKey}/nodes?ids={ids}", fileKey, ids)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new SourceFetchFailedException("Figma 노드 조회에 실패했습니다");
        }
    }
}
