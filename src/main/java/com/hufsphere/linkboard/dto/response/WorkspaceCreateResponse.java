package com.hufsphere.linkboard.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record WorkspaceCreateResponse(
        Long workspaceId,
        String name,
        Long ownerUserId,
        String inviteCode,
        LocalDateTime inviteCodeExpiresAt,
        List<SourceInfo> sources,
        LocalDateTime createdAt
) {

    public record SourceInfo(
            Long sourceId,
            String sourceType,
            String sourceRef,
            String connStatus
    ) {
    }
}