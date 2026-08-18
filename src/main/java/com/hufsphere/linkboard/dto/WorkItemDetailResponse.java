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
public class WorkItemDetailResponse {

    private Long id;
    private String sourceType;
    private String itemType;
    private Long sourceNumber;
    private String title;
    private String status;
    private String authorLogin;
    private String sourceUrl;
    private LocalDateTime sourceUpdatedAt;
    private String summaryNative;
    private List<LinkedItemResponse> linkedItems;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedItemResponse {
        private Long id;
        private String sourceType;
        private String itemType;
        private String title;
        private String sourceUrl;
    }
}