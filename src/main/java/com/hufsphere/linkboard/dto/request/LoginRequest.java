package com.hufsphere.linkboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "로그인 요청")
public class LoginRequest {

    @Schema(description = "로그인 이메일", example = "jaeyoung123@hufs.ac.kr")
    @NotBlank(message = "email과 password는 필수입니다")
    @Email(message = "올바른 이메일 형식이어야 합니다")
    private String email;

    @Schema(description = "비밀번호", example = "Password123!")
    @NotBlank(message = "email과 password는 필수입니다")
    private String password;
}
