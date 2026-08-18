package com.hufsphere.linkboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GithubOAuthClientConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 60_000;

    // GitHub는 토큰 교환은 github.com, 나머지 API는 api.github.com을 쓰므로
    // baseUrl 없이 호출부에서 절대 URI를 지정한다.
    @Bean
    public RestClient githubRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
