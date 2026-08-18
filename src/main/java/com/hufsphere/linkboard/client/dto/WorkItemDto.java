package com.hufsphere.linkboard.client.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkItemDto {

    private String sourceType;
    private String itemType;
    private String title;
    private String summaryBrief;
    private String status;
    private String url;

    // AI 응답에 없을 수 있는 필드 (없으면 null로 저장)
    private Long sourceNumber;
    private String authorLogin;
    private LocalDateTime sourceUpdatedAt;
    private String content;
}
