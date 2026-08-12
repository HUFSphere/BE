package com.hufsphere.linkboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "동기화할 Notion 페이지")
public class NotionPageRequest {

    @Schema(description = "페이지 제목", example = "스프린트 회고")
    private String title;

    @Schema(description = "페이지 URL", example = "https://notion.so/abcdef")
    private String url;

    @Schema(description = "페이지 본문 텍스트")
    private String text;

    @Schema(description = "작업 항목 타입", example = "meeting")
    private String itemType;
}
