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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class FigmaCrawlerClient {

    private static final String FIGMA_API_BASE = "https://api.figma.com";

    // Figma 코멘트 reactions[].emoji는 ":white_check_mark:" 셧코드로 내려오는데,
    // 혹시 유니코드 문자로 오는 경우까지 대비해 둘 다 인정한다.
    private static final Set<String> CHECK_MARK_EMOJIS = Set.of(":white_check_mark:", "✅");

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
            comments.add(new FigmaComment(frameName, url, message, hasCheckMarkReaction(comment)));
        }

        return comments;
    }

    private boolean hasCheckMarkReaction(JsonNode comment) {
        for (JsonNode reaction : comment.path("reactions")) {
            if (CHECK_MARK_EMOJIS.contains(reaction.path("emoji").asString(""))) {
                return true;
            }
        }
        return false;
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
            logFigmaError("코멘트 조회", fileKey, e);
            throw new SourceFetchFailedException("Figma 코멘트 조회에 실패했습니다: " + figmaErrorDetail(e));
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
            logFigmaError("노드 조회", fileKey, e);
            throw new SourceFetchFailedException("Figma 노드 조회에 실패했습니다: " + figmaErrorDetail(e));
        }
    }

    private void logFigmaError(String step, String fileKey, RestClientException e) {
        if (e instanceof RestClientResponseException responseException) {
            log.error("Figma {} 실패: fileKey={}, status={}, body={}", step, fileKey,
                    responseException.getStatusCode(), responseException.getResponseBodyAsString());
        } else {
            log.error("Figma {} 실패: fileKey={}", step, fileKey, e);
        }
    }

    private String figmaErrorDetail(RestClientException e) {
        if (e instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode() + " " + responseException.getResponseBodyAsString();
        }
        return String.valueOf(e.getMessage());
    }
}
