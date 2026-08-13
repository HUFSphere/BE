package com.hufsphere.linkboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemSummaryResponse {

    private Long workItemId;
    private String lang;
    private String summaryText;
    private LocalDateTime createdAt;
}