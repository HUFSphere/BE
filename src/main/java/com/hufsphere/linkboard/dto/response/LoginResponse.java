package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "로그인 응답")
public class LoginResponse {

    @Schema(description = "사용자 ID", example = "1")
    private final Long userId;

    @Schema(description = "로그인 아이디", example = "jaeyoung123")
    private final String username;

    @Schema(description = "이름", example = "박재영")
    private final String name;

    @Schema(description = "모국어", example = "ko")
    private final String nativeLang;

    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private final String accessToken;

    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
    private final String refreshToken;

    public static LoginResponse of(User user, String accessToken, String refreshToken) {
        return new LoginResponse(user.getId(), user.getUsername(), user.getName(), user.getNativeLang(), accessToken, refreshToken);
    }
}
