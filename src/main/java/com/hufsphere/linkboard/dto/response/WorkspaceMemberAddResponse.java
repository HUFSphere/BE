package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.WorkspaceMember;
import java.time.LocalDateTime;

public record WorkspaceMemberAddResponse(
        Long membershipId,
        Long workspaceId,
        Long userId,
        String role,
        LocalDateTime joinedAt
) {

    public static WorkspaceMemberAddResponse from(
            WorkspaceMember membership
    ) {
        return new WorkspaceMemberAddResponse(
                membership.getId(),
                membership.getWorkspace().getId(),
                membership.getUser().getId(),
                membership.getRole(),
                membership.getJoinedAt()
        );
    }
}