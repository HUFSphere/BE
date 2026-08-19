package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.response.GithubRepoResponse;
import com.hufsphere.linkboard.service.GithubOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Tag(name = "Github OAuth", description = "GitHub OAuth 연결 API")
@RestController
@RequestMapping("/api/v1/auth/github")
@RequiredArgsConstructor
public class GithubOAuthController {

    private static final String GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_SCOPE = "repo";
    private static final String STATE_PREFIX = "ws-";
    // FE가 이 경로에서 code/state 처리 결과를 받는다. status=success면 바로 GET /repos를 호출해
    // 레포 선택 화면을 띄우고, status=error면 실패 안내를 띄우면 된다.
    private static final String CALLBACK_LANDING_PATH = "/oauth/github/callback";

    private final GithubOAuthService githubOAuthService;

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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

    @Operation(
            summary = "GitHub 연결 콜백",
            description = "GitHub 인가 코드를 액세스 토큰으로 교환해 워크스페이스에 저장한 뒤, FE의 "
                    + CALLBACK_LANDING_PATH + " 로 리다이렉트한다(JSON을 직접 반환하지 않음). "
                    + "FE는 쿼리 파라미터 workspaceId, status(success|error)를 읽어 success면 "
                    + "GET /api/v1/auth/github/repos?workspaceId= 를 호출해 레포 선택 화면을 띄우면 된다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "처리 완료 후 FE로 리다이렉트. 예: "
                            + "http://localhost:5173/oauth/github/callback?workspaceId=1&status=success"),
    })
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @Parameter(description = "GitHub가 발급한 인가 코드")
            @RequestParam String code,
            @Parameter(description = "authorize 호출 시 실어 보낸 state (ws-{workspaceId})", example = "ws-1")
            @RequestParam String state
    ) {
        Long workspaceId = Long.parseLong(state.substring(STATE_PREFIX.length()));

        String status = "success";
        try {
            githubOAuthService.connect(workspaceId, code);
        } catch (RuntimeException ex) {
            log.error("GitHub 콜백 처리 실패: workspaceId={}", workspaceId, ex);
            status = "error";
        }

        URI landingUri = UriComponentsBuilder.fromUriString(frontendUrl)
                .path(CALLBACK_LANDING_PATH)
                .queryParam("workspaceId", workspaceId)
                .queryParam("status", status)
                .build()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(landingUri)
                .build();
    }

    // TODO: 인증 도입 후 workspaceId에 대한 사용자 접근 권한 검증 추가
    @Operation(
            summary = "GitHub 레포지토리 목록 조회",
            description = "연결된 GitHub 계정이 접근 가능한 레포지토리 목록을 조회한다. 여기서 고른 fullName을 "
                    + "POST /api/v1/workspaces/{workspaceId}/source-connections 의 targetRepoOrBoard로 그대로 보내면 된다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "레포지토리 목록 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "GITHUB_REPOS_OK",
                              "message": "GitHub 레포지토리 목록 조회 성공",
                              "data": [
                                {
                                  "fullName": "HUFSphere/BE",
                                  "privateRepo": false,
                                  "defaultBranch": "main"
                                }
                              ]
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이 워크스페이스에 GitHub가 연결되어 있지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-19T10:00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "먼저 GitHub를 연결해주세요",
                                      "path": "/api/v1/auth/github/repos"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "GitHub 레포지토리 목록 조회 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-19T10:00:00",
                                      "status": 502,
                                      "error": "Bad Gateway",
                                      "message": "GitHub 레포지토리 목록 조회에 실패했습니다",
                                      "path": "/api/v1/auth/github/repos"
                                    }"""))),
    })
    @GetMapping("/repos")
    public ResponseEntity<ApiResponse<List<GithubRepoResponse>>> listRepos(
            @Parameter(description = "GitHub가 연결된 워크스페이스 ID", example = "1")
            @RequestParam Long workspaceId
    ) {
        List<GithubRepoResponse> response = githubOAuthService.listRepos(workspaceId);
        return ResponseEntity.ok(ApiResponse.success("GITHUB_REPOS_OK", "GitHub 레포지토리 목록 조회 성공", response));
    }
}
