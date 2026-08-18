package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.WorkspaceMember;
import java.time.LocalDateTime;

public record WorkspaceMemberResponse(
        Long membershipId,
        Long userId,
        String name,
        String role,
        LocalDateTime joinedAt
) {

    public static WorkspaceMemberResponse from(WorkspaceMember membership) {
        return new WorkspaceMemberResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getName(),
                membership.getRole(),
                membership.getJoinedAt()
        );
    }
}
