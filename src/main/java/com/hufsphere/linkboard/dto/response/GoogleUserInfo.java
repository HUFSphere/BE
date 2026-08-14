package com.hufsphere.linkboard.dto.response;

public record GoogleUserInfo(
        String sub,
        String name,
        String email,
        String picture
) {
}