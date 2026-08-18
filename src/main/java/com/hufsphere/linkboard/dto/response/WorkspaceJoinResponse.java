package com.hufsphere.linkboard.dto.response;

public record WorkspaceJoinResponse(
        Long workspaceId,
        String name,
        String role
) {
}
