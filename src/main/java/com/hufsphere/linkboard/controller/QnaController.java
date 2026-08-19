package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.request.QnaRequest;
import com.hufsphere.linkboard.dto.response.QnaResponse;
import com.hufsphere.linkboard.security.JwtProvider;
import com.hufsphere.linkboard.service.QnaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "QnA", description = "히스토리 기반 Q&A API")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;
    private final JwtProvider jwtProvider;

    // TODO: 인증 도입 후 Authorization 헤더 기반 워크스페이스 접근 권한 검증 추가
    @Operation(summary = "히스토리 기반 Q&A", description = "AI 서버의 검색·생성 엔드포인트(POST /ask)를 호출해 사용자의 모국어 질문에 답한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "답변 생성 성공",
                    content = @Content(schema = @Schema(implementation = QnaResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "QNA_SUCCESS",
                                      "message": "답변이 생성되었습니다",
                                      "data": {
                                        "answer": "Vì lý do mở rộng stateless, nhóm đã chọn JWT thay cho session...",
                                        "sources": [
                                          {
                                            "sourceType": "github",
                                            "itemType": "pr",
                                            "title": "Add JWT auth",
                                            "url": "https://github.com/org/repo/pull/142"
                                          }
                                        ],
                                        "teamNorm": null
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "question 또는 lang 누락",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T15:20:00.000+00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "question과 lang은 필수입니다",
                                      "path": "/api/v1/workspaces/1/qna"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T15:20:00.000+00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1/qna"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "AI 서버 호출 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T15:20:00.000+00:00",
                                      "status": 502,
                                      "error": "Bad Gateway",
                                      "message": "AI 서버 호출에 실패했습니다",
                                      "path": "/api/v1/workspaces/1/qna"
                                    }"""))),
    })
    @PostMapping
    public ResponseEntity<ApiResponse<QnaResponse>> ask(
            @Parameter(description = "질문 대상 워크스페이스 ID", example = "1")
            @PathVariable Long workspaceId,

            @Parameter(description = "Bearer Access Token. 있으면 저장된 톤 설정을 반영하고, 없으면 기본 톤(beginner)을 사용한다", example = "Bearer eyJ...")
            @RequestHeader(value = "Authorization", required = false)
            String authorization,

            @Valid @RequestBody QnaRequest request
    ) {
        Long requesterId = extractUserIdOrNull(authorization);
        QnaResponse response = qnaService.ask(workspaceId, requesterId, request);
        return ResponseEntity.ok(ApiResponse.success("QNA_SUCCESS", "답변이 생성되었습니다", response));
    }

    // Q&A는 아직 인증 필터가 도입되지 않아 로그인 없이도 열려 있다. 토큰이 있으면 톤 설정 개인화에만 사용하고,
    // 없거나 유효하지 않아도 요청 자체는 막지 않는다(기본 톤으로 대체).
    private Long extractUserIdOrNull(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }

        try {
            return jwtProvider.getUserIdFromToken(authorization.substring(7));
        } catch (Exception ex) {
            return null;
        }
    }
}
