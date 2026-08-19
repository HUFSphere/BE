package com.hufsphere.linkboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSourcesResponse {

    private List<SourceCard> sources;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceCard {
        private Long sourceId;
        private String sourceType;
        private String sourceRef;
        private String connStatus;
        private int totalCount;
        private int doneCount;
        private double progress;
        private List<RecentIssue> recentIssues;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentIssue {
        private Long workItemId;
        private String title;
        private String status;
        private String sourceUrl;
        private LocalDateTime sourceUpdatedAt;
    }
}
