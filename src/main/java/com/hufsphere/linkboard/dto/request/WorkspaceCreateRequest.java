package com.hufsphere.linkboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceCreateRequest(

        @NotBlank(message = "워크스페이스 이름은 1~50자여야 합니다")
        @Size(min = 1, max = 50, message = "워크스페이스 이름은 1~50자여야 합니다")
        String name,

        String githubRepo,

        String notionUrl,

        String figmaUrl
) {
}