package com.hufsphere.linkboard.dto.response;

import java.time.LocalDateTime;

public record WorkspaceDetailResponse(
        Long workspaceId,
        String name,
        Long ownerUserId,
        String myRole,
        Integer memberCount,
        Integer sourceCount,
        LocalDateTime createdAt
) {
}