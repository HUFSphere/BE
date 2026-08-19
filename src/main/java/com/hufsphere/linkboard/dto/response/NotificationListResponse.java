package com.hufsphere.linkboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 목록")
public class NotificationListResponse {

    @Schema(description = "최근순으로 정렬된 알림 목록(limit개까지)")
    private List<NotificationResponse> notifications;

    @Schema(description = "읽지 않은 알림 개수(전체 기준, limit과 무관)", example = "3")
    private long unreadCount;
}
