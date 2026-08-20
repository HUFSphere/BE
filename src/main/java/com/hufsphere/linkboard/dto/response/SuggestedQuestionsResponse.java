package com.hufsphere.linkboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SuggestedQuestionsResponse {

    @Schema(description = "신규 합류자가 물어볼 만한 추천 질문 목록")
    private final List<String> questions;
}
