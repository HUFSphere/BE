package com.hufsphere.linkboard.dto.response;

public record OAuthLoginResponse(
        Long userId,
        String name,
        String oauthProvider,
        String nativeLang,
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
}