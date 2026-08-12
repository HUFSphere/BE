package com.hufsphere.linkboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "소스 동기화 요청 (github는 생략, notion은 pages, figma는 comments 필요)")
public class SourceSyncRequest {

    @Schema(description = "notion 소스일 때 전달할 페이지 목록")
    private List<NotionPageRequest> pages;

    @Schema(description = "figma 소스일 때 전달할 코멘트 목록")
    private List<FigmaCommentRequest> comments;
}
