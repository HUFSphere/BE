package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.request.SourceSyncRequest;
import com.hufsphere.linkboard.dto.response.SourceSyncResponse;
import com.hufsphere.linkboard.service.SourceSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Source Sync", description = "소스 동기화 API")
@RestController
@RequestMapping("/api/v1/sources/{sourceId}")
@RequiredArgsConstructor
public class SourceSyncController {

    private final SourceSyncService sourceSyncService;

    // TODO: 인증 도입 후 Authorization 헤더 기반 검증 추가
    @Operation(summary = "소스 동기화", description = "AI 서버의 수집·인덱싱 엔드포인트(POST /ingest/{github|notion|figma})를 호출해 소스를 동기화한다. "
            + "현재는 동기 방식으로 처리되며(호출이 끝날 때까지 대기 후 응답), "
            + "github는 body 없이 sourceRef만으로 수집하고, notion은 body의 pages, figma는 body의 comments를 그대로 AI 서버에 전달한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "동기화 완료",
                    content = @Content(schema = @Schema(implementation = SourceSyncResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SYNC_STARTED",
                                      "message": "동기화를 시작했습니다",
                                      "data": {
                                        "sourceId": 1,
                                        "connStatus": "done",
                                        "startedAt": "2026-08-09T15:12:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "notion/figma 동기화에 필요한 pages/comments 누락",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T15:12:00.000+00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "notion 동기화에는 pages가 필요합니다",
                                      "path": "/api/v1/sources/1/sync"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "소스 연결을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T15:12:00.000+00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "소스 연결을 찾을 수 없습니다",
                                      "path": "/api/v1/sources/1/sync"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 동기화가 진행 중",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T15:12:00.000+00:00",
                                      "status": 409,
                                      "error": "Conflict",
                                      "message": "이미 동기화가 진행 중입니다",
                                      "path": "/api/v1/sources/1/sync"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "AI 서버 호출 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T15:12:00.000+00:00",
                                      "status": 502,
                                      "error": "Bad Gateway",
                                      "message": "외부 소스 조회에 실패했습니다",
                                      "path": "/api/v1/sources/1/sync"
                                    }"""))),
    })
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<SourceSyncResponse>> sync(
            @Parameter(description = "동기화할 소스 연결 ID", example = "1")
            @PathVariable Long sourceId,
            @Parameter(description = "github는 생략, notion은 pages, figma는 comments 필요")
            @RequestBody(required = false) SourceSyncRequest request
    ) {
        SourceSyncResponse response = sourceSyncService.sync(sourceId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("SYNC_STARTED", "동기화를 시작했습니다", response));
    }
}
