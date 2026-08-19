package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.request.ToneSettingRequest;
import com.hufsphere.linkboard.dto.response.ToneSettingResponse;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.security.JwtProvider;
import com.hufsphere.linkboard.service.ToneSettingService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ToneSetting", description = "워크스페이스별 사용자 톤 설정 API")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/tone-setting")
@RequiredArgsConstructor
public class ToneSettingController {

    private final ToneSettingService toneSettingService;
    private final JwtProvider jwtProvider;

    @Operation(
            summary = "톤 설정 조회",
            description = "요청자가 이 워크스페이스에 저장한 톤 설정을 조회한다. 저장된 설정이 없으면 기본값(presetKeys: [\"beginner\"], customText null)을 에러 없이 반환한다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "톤 설정 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "TONE_SETTING_OK",
                              "message": "톤 설정 조회 성공",
                              "data": {
                                "presetKeys": ["beginner", "expert"],
                                "customText": "특히 Spring 관련 결정은 더 자세히 설명해주세요",
                                "updatedAt": "2026-08-19T10:00:00"
                              }
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이 워크스페이스의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-19T10:00:00",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "이 워크스페이스에 접근 권한이 없습니다",
                                      "path": "/api/v1/workspaces/1/tone-setting"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-19T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1/tone-setting"
                                    }"""))),
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ToneSettingResponse>> getToneSetting(
            @PathVariable Long workspaceId,

            @Parameter(description = "Bearer Access Token", required = true, example = "Bearer eyJ...")
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        Long userId = extractUserId(authorization);

        ToneSettingResponse response = toneSettingService.getToneSetting(workspaceId, userId);

        return ResponseEntity.ok(ApiResponse.success("TONE_SETTING_OK", "톤 설정 조회 성공", response));
    }

    @Operation(
            summary = "톤 설정 저장",
            description = "요청자의 톤 설정을 저장한다. presetKeys는 beginner/intermediate/expert 중 하나 이상 중복 선택할 수 있다. 기존 설정이 있으면 갱신한다(upsert)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "톤 설정 저장 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "TONE_SETTING_SAVED",
                              "message": "톤 설정 저장 성공",
                              "data": {
                                "presetKeys": ["beginner", "expert"],
                                "customText": "특히 Spring 관련 결정은 더 자세히 설명해주세요",
                                "updatedAt": "2026-08-19T10:00:00"
                              }
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "presetKeys가 비어있거나 유효하지 않은 값을 포함함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-19T10:00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "presetKeys는 beginner/intermediate/expert로만 구성되어야 합니다",
                                      "path": "/api/v1/workspaces/1/tone-setting"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이 워크스페이스의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-19T10:00:00",
                                      "status": 403,
                                      "error": "Forbidden",
                                      "message": "이 워크스페이스에 접근 권한이 없습니다",
                                      "path": "/api/v1/workspaces/1/tone-setting"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-19T10:00:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1/tone-setting"
                                    }"""))),
    })
    @PutMapping
    public ResponseEntity<ApiResponse<ToneSettingResponse>> saveToneSetting(
            @PathVariable Long workspaceId,

            @Parameter(description = "Bearer Access Token", required = true, example = "Bearer eyJ...")
            @RequestHeader(value = "Authorization", required = false)
            String authorization,

            @Valid @RequestBody ToneSettingRequest request
    ) {
        Long userId = extractUserId(authorization);

        ToneSettingResponse response = toneSettingService.saveToneSetting(workspaceId, userId, request);

        return ResponseEntity.ok(ApiResponse.success("TONE_SETTING_SAVED", "톤 설정 저장 성공", response));
    }

    private Long extractUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("로그인이 필요합니다");
        }

        try {
            return jwtProvider.getUserIdFromToken(authorization.substring(7));
        } catch (Exception ex) {
            throw new InvalidCredentialsException("로그인이 필요합니다");
        }
    }
}
