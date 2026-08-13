package com.hufsphere.linkboard.client;

import com.hufsphere.linkboard.dto.response.GoogleTokenResponse;
import com.hufsphere.linkboard.dto.response.GoogleUserInfo;
import com.hufsphere.linkboard.exception.OAuthAuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GoogleOAuthClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleOAuthClient(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret,
            @Value("${oauth.google.redirect-uri}") String redirectUri
    ) {
        this.restClient = RestClient.create();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public GoogleTokenResponse exchangeCode(String oauthCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("code", oauthCode);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        try {
            return restClient
                    .post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenResponse.class);

        } catch (RestClientResponseException e) {
            throw new OAuthAuthenticationException(
                    "OAuth 인증에 실패했습니다. 인가 코드가 유효하지 않습니다",
                    e
            );
        }
    }

    public GoogleUserInfo getUserInfo(String accessToken) {
        try {
            return restClient
                    .get()
                    .uri("https://openidconnect.googleapis.com/v1/userinfo")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(GoogleUserInfo.class);

        } catch (RestClientResponseException e) {
            throw new OAuthAuthenticationException(
                    "OAuth 인증에 실패했습니다. 사용자 정보를 불러올 수 없습니다",
                    e
            );
        }
    }
}