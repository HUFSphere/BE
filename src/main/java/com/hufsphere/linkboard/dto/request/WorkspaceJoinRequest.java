package com.hufsphere.linkboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WorkspaceJoinRequest(

        @NotBlank(message = "초대 코드 형식이 올바르지 않습니다")
        @Pattern(
                regexp = "^[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$",
                message = "초대 코드 형식이 올바르지 않습니다"
        )
        String inviteCode
) {
}
