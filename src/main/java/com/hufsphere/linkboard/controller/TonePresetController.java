package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.response.TonePresetResponse;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "TonePreset", description = "Q&A 답변 톤 프리셋 API")
@RestController
@RequestMapping("/api/v1/tone-presets")
@RequiredArgsConstructor
public class TonePresetController {

    private final ToneSettingService toneSettingService;
    private final JwtProvider jwtProvider;

    @Operation(
            summary = "톤 프리셋 목록 조회",
            description = "lang이 없으면 요청자의 모국어 기준 프리셋 3개를 즉시 반환한다(AI 서버 호출 없음). lang을 지정하면 로그인 없이도 조회할 수 있다. 한국어(ko)/영어(en)만 지원하며, 그 외 언어는 영어로 대체된다(다른 UI 요소와 동일한 언어 범위)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "톤 프리셋 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "TONE_PRESETS_OK",
                              "message": "톤 프리셋 조회 성공",
                              "data": [
                                {
                                  "presetKey": "concise",
                                  "label": "간결하게"
                                },
                                {
                                  "presetKey": "detailed",
                                  "label": "자세하게"
                                },
                                {
                                  "presetKey": "friendly",
                                  "label": "친근하게"
                                }
                              ]
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "lang 미지정 + 미인증",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-19T10:00:00",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "로그인이 필요합니다",
                                      "path": "/api/v1/tone-presets"
                                    }"""))),
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<TonePresetResponse>>> getPresets(
            @Parameter(description = "프리셋 언어. 없으면 요청자의 모국어", example = "ko")
            @RequestParam(required = false) String lang,

            @Parameter(description = "Bearer Access Token (lang 미지정 시 필수)", example = "Bearer eyJ...")
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        Long requesterId = (lang == null || lang.isBlank()) ? extractUserId(authorization) : null;

        List<TonePresetResponse> response = toneSettingService.getPresets(requesterId, lang);

        return ResponseEntity.ok(ApiResponse.success("TONE_PRESETS_OK", "톤 프리셋 조회 성공", response));
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
