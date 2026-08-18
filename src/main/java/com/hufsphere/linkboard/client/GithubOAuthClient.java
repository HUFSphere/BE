package com.hufsphere.linkboard.client;

import com.hufsphere.linkboard.client.dto.GithubTokenExchangeResponse;
import com.hufsphere.linkboard.client.dto.GithubUserResponse;
import com.hufsphere.linkboard.exception.GithubOAuthFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GithubOAuthClient {

    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";

    private final RestClient githubRestClient;

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    public GithubTokenExchangeResponse exchangeToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        try {
            return githubRestClient.post()
                    .uri(TOKEN_URL)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GithubTokenExchangeResponse.class);
        } catch (RestClientException e) {
            logGithubError("토큰 교환", e);
            throw new GithubOAuthFailedException("GitHub 인증에 실패했습니다");
        }
    }

    public GithubUserResponse fetchUser(String accessToken) {
        try {
            return githubRestClient.get()
                    .uri(USER_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GithubUserResponse.class);
        } catch (RestClientException e) {
            logGithubError("사용자 정보 조회", e);
            throw new GithubOAuthFailedException("GitHub 사용자 정보 조회에 실패했습니다");
        }
    }

    private void logGithubError(String step, RestClientException e) {
        if (e instanceof RestClientResponseException responseException) {
            log.error("GitHub {} 실패: status={}, body={}", step,
                    responseException.getStatusCode(), responseException.getResponseBodyAsString());
        } else {
            log.error("GitHub {} 실패", step, e);
        }
    }
}
