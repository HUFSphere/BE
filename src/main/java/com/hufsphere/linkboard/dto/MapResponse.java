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
        private String statusLabel;
        // Notion to_do 체크 비율(0~100). 신호가 없으면(체크리스트 없는 페이지 등) null.
        private Integer completionRate;
        // WorkItem.summaryNative 컬럼에 저장된 값 (AI가 준 summary_brief를 그대로 담고 있음)
        private String summaryBrief;
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
        private String linkReason;
    }
}