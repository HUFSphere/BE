package com.hufsphere.linkboard.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(

        @NotBlank(message = "oauthProvider는 필수입니다.")
        String oauthProvider,

        @NotBlank(message = "oauthCode는 필수입니다.")
        String oauthCode,

        String nativeLang
) {
    public String resolvedNativeLang() {
        return nativeLang == null || nativeLang.isBlank()
                ? "ko"
                : nativeLang;
    }
}