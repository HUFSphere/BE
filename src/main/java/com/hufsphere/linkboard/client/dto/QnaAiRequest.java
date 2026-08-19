package com.hufsphere.linkboard.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// BE가 AI 서버 POST /qna(전역 검색 대신 명시적 chunks를 받는 엔드포인트)를 호출할 때 쓰는 요청 바디.
// 특정 work item으로 질문 범위를 좁힐 때(QnaService의 contextWorkItemIds) 사용한다.
@Getter
@RequiredArgsConstructor
public class QnaAiRequest {

    private final String question;
    private final String lang;
    private final String tone;

    @JsonProperty("team_norms")
    private final List<TeamNormInputDto> teamNorms;

    private final List<AiChunkDto> chunks;
}
