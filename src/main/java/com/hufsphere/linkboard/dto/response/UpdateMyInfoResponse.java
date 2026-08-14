package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.AppUser;

public record UpdateMyInfoResponse(
        Long userId,
        String name,
        String nativeLang
) {

    public static UpdateMyInfoResponse from(AppUser user) {
        return new UpdateMyInfoResponse(
                user.getId(),
                user.getName(),
                user.getNativeLang()
        );
    }
}