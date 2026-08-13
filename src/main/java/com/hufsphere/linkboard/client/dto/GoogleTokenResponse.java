package com.hufsphere.linkboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("id_token")
        String idToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("scope")
        String scope,

        @JsonProperty("refresh_token")
        String refreshToken
) {
}