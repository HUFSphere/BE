package com.hufsphere.linkboard.dto;

import java.time.LocalDateTime;

public record RecentActivityDto(
        Long workItemId,
        String sourceType,
        String itemType,
        String title,
        String status,
        String sourceUrl,
        LocalDateTime sourceUpdatedAt
) {}