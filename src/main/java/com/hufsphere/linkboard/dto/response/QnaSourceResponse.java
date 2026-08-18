package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.client.dto.AskSourceItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "답변 근거")
public class QnaSourceResponse {

    @Schema(description = "출처 종류", example = "github")
    private final String sourceType;

    @Schema(description = "출처 항목 종류", example = "pr")
    private final String itemType;

    @Schema(description = "제목", example = "Add JWT auth")
    private final String title;

    @Schema(description = "URL", example = "https://github.com/org/repo/pull/142")
    private final String url;

    public static QnaSourceResponse from(AskSourceItem item) {
        return new QnaSourceResponse(item.getSourceType(), item.getItemType(), item.getTitle(), item.getUrl());
    }
}
