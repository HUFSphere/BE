package com.hufsphere.linkboard.client;

import tools.jackson.databind.JsonNode;
import com.hufsphere.linkboard.client.dto.NotionCrawledPage;
import com.hufsphere.linkboard.exception.SourceFetchFailedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class NotionCrawlerClient {

    private static final String NOTION_VERSION = "2022-06-28";
    public static final String UNTITLED_PLACEHOLDER = "(제목 없음)";
    private static final List<String> TEXT_BLOCK_TYPES = List.of(
            "paragraph", "heading_1", "heading_2", "heading_3",
            "bulleted_list_item", "numbered_list_item", "to_do", "quote", "callout"
    );

    private final RestClient notionRestClient;

    public List<NotionCrawledPage> searchPages(String accessToken) {
        List<NotionCrawledPage> pages = new ArrayList<>();
        String cursor = null;

        do {
            JsonNode body = search(accessToken, cursor);
            for (JsonNode result : body.path("results")) {
                if (!"page".equals(result.path("object").asString())) {
                    continue;
                }
                String id = result.path("id").asString();
                String url = result.path("url").asString();
                String title = extractTitle(result.path("properties"));
                pages.add(new NotionCrawledPage(id, title, url));
            }
            cursor = body.path("has_more").asBoolean(false) ? body.path("next_cursor").asString(null) : null;
        } while (cursor != null);

        return pages;
    }

    // text: 본문 텍스트. todoTotal/todoChecked: 페이지 안 to_do 블록 개수와 그중 체크된 개수
    // (완료율 계산용). Notion API가 to_do 블록마다 이미 내려주는 checked 필드를 같은 순회에서
    // 같이 읽는 것뿐이라 추가 API 호출은 없다.
    public record NotionPageContent(String text, int todoTotal, int todoChecked) {}

    public NotionPageContent fetchPageText(String accessToken, String pageId) {
        StringBuilder text = new StringBuilder();
        int todoTotal = 0;
        int todoChecked = 0;
        String cursor = null;

        do {
            JsonNode body = fetchBlockChildren(accessToken, pageId, cursor);
            for (JsonNode block : body.path("results")) {
                String type = block.path("type").asString();
                if (!TEXT_BLOCK_TYPES.contains(type)) {
                    continue;
                }
                if ("to_do".equals(type)) {
                    todoTotal++;
                    if (block.path("to_do").path("checked").asBoolean(false)) {
                        todoChecked++;
                    }
                }
                String blockText = joinPlainText(block.path(type).path("rich_text"));
                if (!blockText.isBlank()) {
                    text.append(blockText).append("\n");
                }
            }
            cursor = body.path("has_more").asBoolean(false) ? body.path("next_cursor").asString(null) : null;
        } while (cursor != null);

        return new NotionPageContent(text.toString().trim(), todoTotal, todoChecked);
    }

    private JsonNode search(String accessToken, String cursor) {
        Map<String, Object> body = new HashMap<>();
        body.put("filter", Map.of("property", "object", "value", "page"));
        if (cursor != null) {
            body.put("start_cursor", cursor);
        }

        try {
            return notionRestClient.post()
                    .uri("/v1/search")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("Notion-Version", NOTION_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new SourceFetchFailedException("Notion 페이지 검색에 실패했습니다");
        }
    }

    private JsonNode fetchBlockChildren(String accessToken, String pageId, String cursor) {
        try {
            return notionRestClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/v1/blocks/{pageId}/children");
                        if (cursor != null) {
                            builder = builder.queryParam("start_cursor", cursor);
                        }
                        return builder.build(pageId);
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header("Notion-Version", NOTION_VERSION)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new SourceFetchFailedException("Notion 페이지 본문 조회에 실패했습니다");
        }
    }

    // properties는 데이터베이스마다 title 속성의 key 이름이 달라서(Name, 제목 등),
    // key가 아니라 type == "title"인 속성을 찾아야 한다.
    private String extractTitle(JsonNode properties) {
        for (JsonNode property : properties) {
            if ("title".equals(property.path("type").asString())) {
                String title = joinPlainText(property.path("title"));
                return title.isBlank() ? UNTITLED_PLACEHOLDER : title;
            }
        }
        return UNTITLED_PLACEHOLDER;
    }

    private String joinPlainText(JsonNode richTextArray) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode span : richTextArray) {
            sb.append(span.path("plain_text").asString());
        }
        return sb.toString();
    }
}
