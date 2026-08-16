package com.hufsphere.linkboard.dto;

import java.util.List;

public record RecentActivitiesResponse(
        List<RecentActivityDto> activities
) {}