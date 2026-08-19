package com.hufsphere.linkboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원가입 요청")
public class SignupRequest {

    @Schema(description = "로그인 이메일 (아이디로 사용)", example = "jaeyoung123@hufs.ac.kr")
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이어야 합니다")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다")
    private String email;

    @Schema(description = "비밀번호 (8자 이상)", example = "Password123!")
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;

    @Schema(description = "이름 (1~20자)", example = "박재영")
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 20, message = "이름은 1~20자여야 합니다")
    private String name;

    @Schema(
            description = "모국어. 없으면 ko",
            example = "ko",
            allowableValues = {"en", "ko", "de", "ja", "zh", "es", "ms", "it", "fr", "ar", "ru"}
    )
    @Pattern(
            regexp = "en|ko|de|ja|zh|es|ms|it|fr|ar|ru",
            message = "nativeLang은 en, ko, de, ja, zh, es, ms, it, fr, ar, ru 중 하나여야 합니다"
    )
    private String nativeLang;
}
