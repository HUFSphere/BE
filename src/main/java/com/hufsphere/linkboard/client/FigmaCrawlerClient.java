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
import java.util.function.Supplier;
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

    // 코멘트 조회 직후 텀 없이 노드 조회가 나가면 Figma의 초당 요청 제한(burst limit)에
    // 자주 걸려서, 429를 받으면 짧게 대기 후 한 번만 재시도한다.
    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 1500;

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
        return fetchWithRetry(
                () -> figmaRestClient.get()
                        .uri(FIGMA_API_BASE + "/v1/files/{fileKey}/comments", fileKey)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(JsonNode.class),
                "코멘트 조회", fileKey);
    }

    private JsonNode fetchNodesRaw(String accessToken, String fileKey, String ids) {
        return fetchWithRetry(
                () -> figmaRestClient.get()
                        .uri(FIGMA_API_BASE + "/v1/files/{fileKey}/nodes?ids={ids}", fileKey, ids)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(JsonNode.class),
                "노드 조회", fileKey);
    }

    // 429(rate limit)만 재시도 대상으로 삼는다. 그 외 오류는 재시도해도 결과가 같으므로 바로 던진다.
    private JsonNode fetchWithRetry(Supplier<JsonNode> request, String step, String fileKey) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return request.get();
            } catch (RestClientResponseException e) {
                boolean isLastAttempt = attempt == MAX_ATTEMPTS;
                if (e.getStatusCode().value() != 429 || isLastAttempt) {
                    logFigmaError(step, fileKey, e);
                    throw new SourceFetchFailedException("Figma " + step + "에 실패했습니다: " + figmaErrorDetail(e));
                }
                log.warn("Figma {} 429 rate limit, {}ms 후 재시도: fileKey={}", step, RETRY_DELAY_MS, fileKey);
                sleep(RETRY_DELAY_MS);
            } catch (RestClientException e) {
                logFigmaError(step, fileKey, e);
                throw new SourceFetchFailedException("Figma " + step + "에 실패했습니다: " + figmaErrorDetail(e));
            }
        }
        throw new IllegalStateException("unreachable");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SourceFetchFailedException("Figma 조회가 중단되었습니다");
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
