package com.hufsphere.linkboard.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팀원 초대 코드 발급 응답 DTO")
public class WorkspaceInviteResponse {

    @Schema(description = "워크스페이스 ID", example = "1")
    private Long workspaceId;

    @Schema(description = "초대 코드 (8자리)", example = "XK79-2M9Q")
    private String inviteCode;

    @Schema(description = "초대 코드 만료 일시 (발급 후 7일)", example = "2026-08-21T17:30:00")
    private LocalDateTime expiresAt;
}