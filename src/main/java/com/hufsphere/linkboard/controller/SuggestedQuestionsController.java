package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.response.SuggestedQuestionsResponse;
import com.hufsphere.linkboard.service.SuggestedQuestionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SuggestedQuestions", description = "신규 합류자 추천 질문 API. GitHub/Notion/Figma 동기화 내용을 바탕으로 " +
        "새로 들어온 팀원이 물어볼 만한 질문을 AI가 생성한다. 워크스페이스 단위라 언제 합류했는지와 무관하게 조회 가능하다.")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/suggested-questions")
@RequiredArgsConstructor
public class SuggestedQuestionsController {

    private final SuggestedQuestionsService suggestedQuestionsService;

    @Operation(summary = "추천 질문 조회", description = "AI 서버의 /suggest-questions를 호출해 신규 합류자가 물어볼 만한 질문 3개를 반환한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추천 질문 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuggestedQuestionsResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUGGESTED_QUESTIONS_OK",
                                      "message": "추천 질문 조회 성공",
                                      "data": {
                                        "questions": [
                                          "이 프로젝트는 인증 방식으로 무엇을 쓰나요?",
                                          "최근에 논의된 주요 결정 사항은 무엇인가요?",
                                          "지금 진행 중인 작업은 어떤 것들이 있나요?"
                                        ]
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "AI 서버 호출 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping
    public ResponseEntity<ApiResponse<SuggestedQuestionsResponse>> getSuggestedQuestions(
            @Parameter(description = "워크스페이스 ID", example = "1")
            @PathVariable Long workspaceId
    ) {
        SuggestedQuestionsResponse response = suggestedQuestionsService.getSuggestedQuestions(workspaceId);
        return ResponseEntity.ok(ApiResponse.success("SUGGESTED_QUESTIONS_OK", "추천 질문 조회 성공", response));
    }
}
