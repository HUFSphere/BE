package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.AppUser;

import java.time.LocalDateTime;

public record MyInfoResponse(
        Long userId,
        String name,
        String oauthProvider,
        String nativeLang,
        LocalDateTime createdAt
) {

    public static MyInfoResponse from(AppUser user) {
        return new MyInfoResponse(
                user.getId(),
                user.getName(),
                user.getOauthProvider(),
                user.getNativeLang(),
                user.getCreatedAt()
        );
    }
}