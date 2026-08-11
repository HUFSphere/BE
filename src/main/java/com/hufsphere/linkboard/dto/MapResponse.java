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
public class MapResponse {

    private List<NodeResponse> nodes;
    private List<LinkResponse> links;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeResponse {
        private Long id;
        private String sourceType;
        private String itemType;
        private Long sourceNumber;
        private String title;
        private String status;
        private String summaryNative;
        private String authorLogin;
        private String sourceUrl;
        private LocalDateTime sourceUpdatedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkResponse {
        private Long fromWorkItemId;
        private Long toWorkItemId;
        private String linkSource;
    }
}