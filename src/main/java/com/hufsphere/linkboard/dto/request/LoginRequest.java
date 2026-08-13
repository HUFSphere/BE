package com.hufsphere.linkboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "로그인 요청")
public class LoginRequest {

    @Schema(description = "로그인 아이디", example = "jaeyoung123")
    @NotBlank(message = "username과 password는 필수입니다")
    private String username;

    @Schema(description = "비밀번호", example = "Password123!")
    @NotBlank(message = "username과 password는 필수입니다")
    private String password;
}
