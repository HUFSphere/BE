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
public class WorkItemPageResponse {

    private List<ItemSummary> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemSummary {
        private Long id;
        private String sourceType;
        private String itemType;
        private Long sourceNumber;
        private String title;
        private String status;
        private String statusLabel;
        private String authorLogin;
        private String sourceUrl;
        private LocalDateTime sourceUpdatedAt;
    }
}