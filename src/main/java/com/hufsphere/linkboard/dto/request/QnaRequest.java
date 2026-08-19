package com.hufsphere.linkboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Q&A 질문 요청")
public class QnaRequest {

    @Schema(description = "사용자의 모국어 질문", example = "왜 세션 대신 JWT를 썼어요?")
    @NotBlank(message = "question과 lang은 필수입니다")
    private String question;

    @Schema(description = "여러 작업(기능) 맥락에서 질문 시 그 작업 ID 목록. 지정하면 이 작업들로만 답변 범위를 좁힌다. 전역이면 생략", example = "[142, 156]")
    private List<Long> contextWorkItemIds;

    @Schema(
            description = "답변 언어",
            example = "ko",
            allowableValues = {"en", "ko", "de", "ja", "zh", "es", "ms", "it", "fr", "ar", "ru"}
    )
    @NotBlank(message = "question과 lang은 필수입니다")
    private String lang;
}
