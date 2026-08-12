package com.hufsphere.linkboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "동기화할 Figma 코멘트")
public class FigmaCommentRequest {

    @Schema(description = "프레임 이름", example = "로그인 화면")
    private String frameName;

    @Schema(description = "코멘트 URL", example = "https://figma.com/file/xyz?node-id=1")
    private String url;

    @Schema(description = "코멘트 내용")
    private String text;
}
