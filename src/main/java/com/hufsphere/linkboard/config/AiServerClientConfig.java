package com.hufsphere.linkboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiServerClientConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 60_000;

    // application.yml에 값이 없을 경우 기본값으로 http://localhost:8000 사용
    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    @Bean
    public RestClient aiServerRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);

        return RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(requestFactory)
                .build();
    }
}