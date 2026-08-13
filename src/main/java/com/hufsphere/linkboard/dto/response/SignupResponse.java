package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.AppUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "회원가입 응답")
public class SignupResponse {

    @Schema(description = "사용자 ID", example = "1")
    private final Long userId;

    @Schema(description = "로그인 아이디", example = "jaeyoung123")
    private final String username;

    @Schema(description = "이름", example = "박재영")
    private final String name;

    @Schema(description = "모국어", example = "ko")
    private final String nativeLang;

    @Schema(description = "가입 시각", example = "2026-08-12T10:00:00")
    private final LocalDateTime createdAt;

    public static SignupResponse from(AppUser user) {
        return new SignupResponse(user.getId(), user.getUsername(), user.getName(), user.getNativeLang(), user.getCreatedAt());
    }
}
