package com.hufsphere.linkboard.client.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FigmaComment {

    private final String frameName;
    private final String url;
    private final String text;

    // ✅ 리액션이 달린 코멘트인지. AI ingest 스키마엔 없는 필드라 그대로 보내도 무시되며,
    // SourceSyncService가 work item 상태를 덮어쓸 때만 사용한다.
    private final boolean done;
}
