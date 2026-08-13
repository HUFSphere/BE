package com.hufsphere.linkboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원가입 요청")
public class SignupRequest {

    @Schema(description = "로그인 아이디 (4~20자)", example = "jaeyoung123")
    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다")
    private String username;

    @Schema(description = "비밀번호 (8자 이상)", example = "Password123!")
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;

    @Schema(description = "이름 (1~20자)", example = "박재영")
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 20, message = "이름은 1~20자여야 합니다")
    private String name;

    @Schema(description = "모국어. 없으면 ko", example = "ko", allowableValues = {"ko", "vi", "en"})
    @Pattern(regexp = "ko|vi|en", message = "지원하지 않는 언어입니다 (ko, vi, en)")
    private String nativeLang;
}
