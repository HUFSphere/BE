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
public class DashboardFeaturesResponse {

    private List<FeatureProgress> features;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureProgress {
        private Long featureId;
        private String name;
        private int totalCount;
        private int doneCount;
        private double progress;
        private LocalDateTime lastUpdatedAt;
    }
}
