package com.hufsphere.linkboard.dto.response;

import java.time.LocalDateTime;

public record WorkspaceListResponse(
        Long workspaceId,
        String name,
        String myRole,
        Long ownerUserId,
        Integer sourceCount,
        LocalDateTime createdAt
) {
}