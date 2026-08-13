package com.hufsphere.linkboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class NotionOAuthClientConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 60_000;
    private static final String NOTION_API_BASE_URL = "https://api.notion.com";

    @Bean
    public RestClient notionRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);

        return RestClient.builder()
                .baseUrl(NOTION_API_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }
}
