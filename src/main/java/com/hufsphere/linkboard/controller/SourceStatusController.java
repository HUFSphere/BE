package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.response.SourceStatusResponse;
import com.hufsphere.linkboard.service.SourceStatusService;
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

@Tag(name = "Source Status", description = "소스 상태 조회 API")
@RestController
@RequestMapping("/api/v1/sources/{sourceId}")
@RequiredArgsConstructor
public class SourceStatusController {

    private final SourceStatusService sourceStatusService;

    // TODO: 인증 도입 후 Authorization 헤더 기반 검증 추가
    @Operation(summary = "소스 상태 조회", description = "소스 연결의 현재 동기화/인덱싱 상태를 조회한다. 프론트는 이 값을 폴링해 진행률을 갱신한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "소스 상태 조회 성공",
                    content = @Content(schema = @Schema(implementation = SourceStatusResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SOURCE_STATUS_OK",
                                      "message": "소스 상태 조회 성공",
                                      "data": {
                                        "sourceId": 1,
                                        "sourceType": "github",
                                        "sourceRef": "pypa/sampleproject",
                                        "connStatus": "indexing",
                                        "indexedCount": 6,
                                        "lastSyncedAt": "2026-08-09T15:12:40"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "소스 연결을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T15:13:00.000+00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "소스 연결을 찾을 수 없습니다",
                                      "path": "/api/v1/sources/1/status"
                                    }"""))),
    })
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SourceStatusResponse>> getStatus(
            @Parameter(description = "상태를 조회할 소스 연결 ID", example = "1")
            @PathVariable Long sourceId
    ) {
        SourceStatusResponse response = sourceStatusService.getStatus(sourceId);
        return ResponseEntity.ok(ApiResponse.success("SOURCE_STATUS_OK", "소스 상태 조회 성공", response));
    }
}
