package com.hufsphere.linkboard.client;

import com.hufsphere.linkboard.client.dto.FigmaTokenExchangeResponse;
import com.hufsphere.linkboard.client.dto.FigmaUserResponse;
import com.hufsphere.linkboard.exception.FigmaOAuthFailedException;
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
public class FigmaOAuthClient {

    private static final String TOKEN_URL = "https://api.figma.com/v1/oauth/token";
    private static final String ME_URL = "https://api.figma.com/v1/me";

    private final RestClient figmaRestClient;

    @Value("${figma.client-id}")
    private String clientId;

    @Value("${figma.client-secret}")
    private String clientSecret;

    @Value("${figma.redirect-uri}")
    private String redirectUri;

    public FigmaTokenExchangeResponse exchangeToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        form.add("grant_type", "authorization_code");

        try {
            return figmaRestClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(FigmaTokenExchangeResponse.class);
        } catch (RestClientException e) {
            logFigmaError("토큰 교환", e);
            throw new FigmaOAuthFailedException("Figma 인증에 실패했습니다");
        }
    }

    public FigmaUserResponse fetchUser(String accessToken) {
        try {
            return figmaRestClient.get()
                    .uri(ME_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(FigmaUserResponse.class);
        } catch (RestClientException e) {
            logFigmaError("사용자 정보 조회", e);
            throw new FigmaOAuthFailedException("Figma 사용자 정보 조회에 실패했습니다");
        }
    }

    private void logFigmaError(String step, RestClientException e) {
        if (e instanceof RestClientResponseException responseException) {
            log.error("Figma {} 실패: status={}, body={}", step,
                    responseException.getStatusCode(), responseException.getResponseBodyAsString());
        } else {
            log.error("Figma {} 실패", step, e);
        }
    }
}
