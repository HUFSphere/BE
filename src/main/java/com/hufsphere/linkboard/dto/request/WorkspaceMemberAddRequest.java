package com.hufsphere.linkboard.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record WorkspaceMemberAddRequest(

        @NotNull(message = "userId는 필수입니다")
        Long userId,

        @Pattern(
                regexp = "leader|member",
                message = "role은 leader 또는 member여야 합니다"
        )
        String role
) {
}