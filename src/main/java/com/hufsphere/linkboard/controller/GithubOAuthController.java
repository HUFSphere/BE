package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.response.GithubOAuthConnectionResponse;
import com.hufsphere.linkboard.service.GithubOAuthService;
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

@Tag(name = "Github OAuth", description = "GitHub OAuth 연결 API")
@RestController
@RequestMapping("/api/v1/auth/github")
@RequiredArgsConstructor
public class GithubOAuthController {

    private static final String GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_SCOPE = "repo";
    private static final String STATE_PREFIX = "ws-";

    private final GithubOAuthService githubOAuthService;

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    // TODO: 인증 도입 후 workspaceId에 대한 사용자 접근 권한 검증 추가
    @Operation(summary = "GitHub 연결 시작", description = "workspaceId를 state로 실어 GitHub 인가 페이지로 리다이렉트한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "GitHub 인가 페이지로 리다이렉트"),
    })
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @Parameter(description = "연결할 워크스페이스 ID", example = "1")
            @RequestParam Long workspaceId
    ) {
        URI authorizeUri = UriComponentsBuilder.fromUriString(GITHUB_AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", GITHUB_SCOPE)
                .queryParam("state", STATE_PREFIX + workspaceId)
                .build()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(authorizeUri)
                .build();
    }

    @Operation(summary = "GitHub 연결 콜백", description = "GitHub 인가 코드를 액세스 토큰으로 교환해 워크스페이스에 저장한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "GitHub 연결 성공",
                    content = @Content(schema = @Schema(implementation = GithubOAuthConnectionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "GITHUB_CONNECTED",
                                      "message": "GitHub가 연결되었습니다",
                                      "data": {
                                        "workspaceId": 1,
                                        "githubLogin": "jaeyoung123"
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
                                      "path": "/api/v1/auth/github/callback"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "GitHub 토큰 교환 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 502,
                                      "error": "Bad Gateway",
                                      "message": "GitHub 인증에 실패했습니다",
                                      "path": "/api/v1/auth/github/callback"
                                    }"""))),
    })
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<GithubOAuthConnectionResponse>> callback(
            @Parameter(description = "GitHub가 발급한 인가 코드")
            @RequestParam String code,
            @Parameter(description = "authorize 호출 시 실어 보낸 state (ws-{workspaceId})", example = "ws-1")
            @RequestParam String state
    ) {
        Long workspaceId = Long.parseLong(state.substring(STATE_PREFIX.length()));
        GithubOAuthConnectionResponse response = githubOAuthService.connect(workspaceId, code);
        return ResponseEntity.ok(ApiResponse.success("GITHUB_CONNECTED", "GitHub가 연결되었습니다", response));
    }
}
