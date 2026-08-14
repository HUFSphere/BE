package com.hufsphere.linkboard.client;

import com.hufsphere.linkboard.client.dto.NotionTokenExchangeRequest;
import com.hufsphere.linkboard.client.dto.NotionTokenExchangeResponse;
import com.hufsphere.linkboard.exception.NotionOAuthFailedException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class NotionOAuthClient {

    private static final String NOTION_VERSION = "2022-06-28";

    private final RestClient notionRestClient;

    @Value("${notion.client-id}")
    private String clientId;

    @Value("${notion.client-secret}")
    private String clientSecret;

    @Value("${notion.redirect-uri}")
    private String redirectUri;

    public NotionTokenExchangeResponse exchangeToken(String code) {
        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        try {
            return notionRestClient.post()
                    .uri("/v1/oauth/token")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                    .header("Notion-Version", NOTION_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NotionTokenExchangeRequest(code, redirectUri))
                    .retrieve()
                    .body(NotionTokenExchangeResponse.class);
        } catch (RestClientException e) {
            throw new NotionOAuthFailedException("Notion 인증에 실패했습니다");
        }
    }
}
