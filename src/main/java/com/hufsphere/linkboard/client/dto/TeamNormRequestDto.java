package com.hufsphere.linkboard.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팀 관행 생성/수정 요청 DTO")
public class TeamNormRequestDto {

    @Schema(description = "관행 카테고리", example = "COMMUNICATION")
    private String category;

    @Schema(description = "관행 내용", example = "오후 6시 이후의 급한 연락은 슬랙 멘션으로 남겨주세요.")
    private String content;
}