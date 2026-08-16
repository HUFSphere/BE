package com.hufsphere.linkboard.client.dto;

import com.hufsphere.linkboard.domain.WorkItem;
import java.time.LocalDateTime;

public record WorkItemResponseDto(
        Long id,
        String sourceType,
        String itemType,
        String title,
        String status,
        String sourceUrl,
        LocalDateTime sourceUpdatedAt
) {
    public static WorkItemResponseDto from(WorkItem workItem) {
        return new WorkItemResponseDto(
                workItem.getId(),
                String.valueOf(workItem.getSourceType()),
                workItem.getItemType(),
                workItem.getTitle(),
                workItem.getStatus(),
                workItem.getSourceUrl(),
                workItem.getSourceUpdatedAt()
        );
    }
}
