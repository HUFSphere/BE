package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.request.SourceConnectionCreateRequest;
import com.hufsphere.linkboard.dto.response.SourceConnectionResponse;
import com.hufsphere.linkboard.service.SourceConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Source Connection", description = "소스 연결 API")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/sources")
@RequiredArgsConstructor
public class SourceConnectionController {

    private final SourceConnectionService sourceConnectionService;

    // TODO: 인증 도입 후 Authorization 헤더 기반 워크스페이스 접근 권한 검증 추가
    @Operation(summary = "소스 연결", description = "워크스페이스에 github/notion/figma 소스 연결 레코드를 생성한다. 실제 수집·인덱싱은 하지 않는다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "소스 연결 성공",
                    content = @Content(schema = @Schema(implementation = SourceConnectionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SOURCE_CONNECTED",
                                      "message": "소스가 연결되었습니다",
                                      "data": {
                                        "sourceId": 1,
                                        "workspaceId": 1,
                                        "sourceType": "github",
                                        "sourceRef": "pypa/sampleproject",
                                        "connStatus": "pending",
                                        "lastSyncedAt": null,
                                        "createdAt": "2026-08-09T15:10:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 sourceType이거나 필수 필드가 비어있음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T15:10:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "지원하지 않는 sourceType입니다 (github, notion, figma)",
                                      "path": "/api/v1/workspaces/1/sources"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T15:10:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없거나 소스에 접근할 수 없습니다",
                                      "path": "/api/v1/workspaces/1/sources"
                                    }"""))),
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SourceConnectionResponse>> connectSource(
            @Parameter(description = "소스를 연결할 워크스페이스 ID", example = "1")
            @PathVariable Long workspaceId,
            @Valid @RequestBody SourceConnectionCreateRequest request
    ) {
        SourceConnectionResponse response = sourceConnectionService.connectSource(workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("SOURCE_CONNECTED", "소스가 연결되었습니다", response));
    }

    // TODO: 인증 도입 후 Authorization 헤더 기반 워크스페이스 접근 권한 검증 추가
    @Operation(summary = "소스 목록 조회", description = "워크스페이스에 연결된 소스 목록을 생성일 순으로 조회한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "소스 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = SourceConnectionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SOURCES_OK",
                                      "message": "소스 목록 조회 성공",
                                      "data": [
                                        {
                                          "sourceId": 1,
                                          "workspaceId": 1,
                                          "sourceType": "github",
                                          "sourceRef": "pypa/sampleproject",
                                          "connStatus": "done",
                                          "lastSyncedAt": "2026-08-09T15:12:40",
                                          "createdAt": "2026-08-09T15:10:00"
                                        }
                                      ]
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T17:15:00.000+00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1/sources"
                                    }"""))),
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<SourceConnectionResponse>>> getSources(
            @Parameter(description = "소스를 조회할 워크스페이스 ID", example = "1")
            @PathVariable Long workspaceId
    ) {
        List<SourceConnectionResponse> response = sourceConnectionService.getSources(workspaceId);
        return ResponseEntity.ok(ApiResponse.success("SOURCES_OK", "소스 목록 조회 성공", response));
    }
}
