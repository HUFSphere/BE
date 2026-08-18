package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.response.FigmaOAuthConnectionResponse;
import com.hufsphere.linkboard.service.FigmaOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Figma OAuth", description = "Figma OAuth 연결 API")
@RestController
@RequestMapping("/api/v1/auth/figma")
@RequiredArgsConstructor
public class FigmaOAuthController {

    private static final String FIGMA_AUTHORIZE_URL = "https://www.figma.com/oauth";
    private static final String FIGMA_SCOPE = "files:read,file_comments:read";
    private static final String STATE_PREFIX = "ws-";

    private final FigmaOAuthService figmaOAuthService;

    @Value("${figma.client-id}")
    private String clientId;

    @Value("${figma.redirect-uri}")
    private String redirectUri;

    // TODO: 인증 도입 후 workspaceId에 대한 사용자 접근 권한 검증 추가
    @Operation(summary = "Figma 연결 시작", description = "workspaceId를 state로 실어 Figma 인가 페이지로 리다이렉트한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "Figma 인가 페이지로 리다이렉트"),
    })
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @Parameter(description = "연결할 워크스페이스 ID", example = "1")
            @RequestParam Long workspaceId
    ) {
        URI authorizeUri = UriComponentsBuilder.fromUriString(FIGMA_AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", FIGMA_SCOPE)
                .queryParam("state", STATE_PREFIX + workspaceId)
                .queryParam("response_type", "code")
                .build()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(authorizeUri)
                .build();
    }

    @Operation(summary = "Figma 연결 콜백", description = "Figma 인가 코드를 액세스 토큰으로 교환해 워크스페이스에 저장한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Figma 연결 성공",
                    content = @Content(schema = @Schema(implementation = FigmaOAuthConnectionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "FIGMA_CONNECTED",
                                      "message": "Figma가 연결되었습니다",
                                      "data": {
                                        "workspaceId": 1,
                                        "figmaUserHandle": "hufsphere"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/auth/figma/callback"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "Figma 토큰 교환 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 502,
                                      "error": "Bad Gateway",
                                      "message": "Figma 인증에 실패했습니다",
                                      "path": "/api/v1/auth/figma/callback"
                                    }"""))),
    })
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<FigmaOAuthConnectionResponse>> callback(
            @Parameter(description = "Figma가 발급한 인가 코드")
            @RequestParam String code,
            @Parameter(description = "authorize 호출 시 실어 보낸 state (ws-{workspaceId})", example = "ws-1")
            @RequestParam String state
    ) {
        Long workspaceId = Long.parseLong(state.substring(STATE_PREFIX.length()));
        FigmaOAuthConnectionResponse response = figmaOAuthService.connect(workspaceId, code);
        return ResponseEntity.ok(ApiResponse.success("FIGMA_CONNECTED", "Figma가 연결되었습니다", response));
    }
}
