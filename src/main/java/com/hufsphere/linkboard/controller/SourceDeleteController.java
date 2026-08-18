package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.security.JwtProvider;
import com.hufsphere.linkboard.service.SourceConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SourceConnection", description = "데이터 출처 연동 및 동기화 API")
@RestController
@RequestMapping("/api/v1/sources/{sourceId}")
@RequiredArgsConstructor
public class SourceDeleteController {

    private final SourceConnectionService sourceConnectionService;
    private final JwtProvider jwtProvider;

    @Operation(
            summary = "소스 연결 해제 (3.5)",
            description = "팀장이 워크스페이스의 데이터 출처 연동을 해제합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "소스 연결 해제 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "SOURCE_CONNECTION_DELETED",
                              "message": "소스 연결이 해제되었습니다",
                              "data": null
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "미인증",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "로그인이 필요합니다",
                                      "path": "/api/v1/sources/1"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "팀장이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "소스 연결 해제는 팀장만 가능합니다",
                                      "path": "/api/v1/sources/1"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "소스 연결을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "소스 연결을 찾을 수 없습니다",
                                      "path": "/api/v1/sources/1"
                                    }"""))),
    })
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteConnection(
            @Parameter(description = "해제할 소스 연결 ID", example = "1")
            @PathVariable Long sourceId,

            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        Long loginUserId = extractUserId(authorization);

        sourceConnectionService.deleteConnection(sourceId, loginUserId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "SOURCE_CONNECTION_DELETED",
                        "소스 연결이 해제되었습니다",
                        null
                )
        );
    }

    private Long extractUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("로그인이 필요합니다");
        }

        String token = authorization.substring(7);

        try {
            return jwtProvider.getUserIdFromToken(token);
        } catch (Exception ex) {
            throw new InvalidCredentialsException("로그인이 필요합니다");
        }
    }
}
