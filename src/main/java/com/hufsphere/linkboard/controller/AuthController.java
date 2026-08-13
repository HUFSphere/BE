package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.dto.request.OAuthLoginRequest;
import com.hufsphere.linkboard.dto.response.OAuthLoginResponse;
import com.hufsphere.linkboard.service.OAuthService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthService oAuthService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    @GetMapping("/oauth/{provider}/authorize")
    public ResponseEntity<Void> startOAuth(
            @PathVariable String provider
    ) {
        if (!provider.equalsIgnoreCase("google")) {
            throw new IllegalArgumentException("지원하지 않는 소셜 제공자입니다.");
        }

        String authorizationUrl = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email")
                .build()
                .encode()
                .toUriString();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(authorizationUrl))
                .build();
    }

    @PostMapping("/oauth")
    public ResponseEntity<ApiResponse<OAuthLoginResponse>> loginOAuth(
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        OAuthLoginResponse response =
                oAuthService.loginOrSignup(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "LOGIN_SUCCESS",
                        "로그인되었습니다",
                        response
                )
        );
    }
}