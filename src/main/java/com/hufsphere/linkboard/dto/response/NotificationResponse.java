package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림")
public class NotificationResponse {

    @Schema(description = "알림 ID", example = "1")
    private Long id;

    @Schema(description = "이벤트 종류", example = "SOURCE_SYNC_SUCCESS")
    private String type;

    @Schema(description = "워크스페이스 ID", example = "1")
    private Long workspaceId;

    @Schema(description = "알림 메시지", example = "GitHub 동기화가 완료되었습니다")
    private String message;

    @Schema(description = "읽음 여부", example = "false")
    private boolean read;

    @Schema(description = "생성 시각", example = "2026-08-20T10:00:00")
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .workspaceId(notification.getWorkspaceId())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
