package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.service.NotionOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
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
@Tag(name = "Notion OAuth", description = "Notion OAuth 연결 API")
@RestController
@RequestMapping("/api/v1/auth/notion")
@RequiredArgsConstructor
public class NotionOAuthController {

    private static final String NOTION_AUTHORIZE_URL = "https://api.notion.com/v1/oauth/authorize";
    private static final String STATE_PREFIX = "ws-";
    // FE가 이 경로에서 code/state 처리 결과를 받는다.
    private static final String CALLBACK_LANDING_PATH = "/oauth/notion/callback";

    private final NotionOAuthService notionOAuthService;

    @Value("${notion.client-id}")
    private String clientId;

    @Value("${notion.redirect-uri}")
    private String redirectUri;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // TODO: 인증 도입 후 workspaceId에 대한 사용자 접근 권한 검증 추가
    @Operation(summary = "Notion 연결 시작", description = "workspaceId를 state로 실어 Notion 인가 페이지로 리다이렉트한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "Notion 인가 페이지로 리다이렉트"),
    })
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @Parameter(description = "연결할 워크스페이스 ID", example = "1")
            @RequestParam Long workspaceId
    ) {
        URI authorizeUri = UriComponentsBuilder.fromUriString(NOTION_AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("owner", "user")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", STATE_PREFIX + workspaceId)
                .build()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(authorizeUri)
                .build();
    }

    @Operation(
            summary = "Notion 연결 콜백",
            description = "Notion 인가 코드를 액세스 토큰으로 교환해 워크스페이스에 저장한 뒤, FE의 "
                    + CALLBACK_LANDING_PATH + " 로 리다이렉트한다(JSON을 직접 반환하지 않음). "
                    + "FE는 쿼리 파라미터 workspaceId, status(success|error)를 읽어 처리하면 된다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "처리 완료 후 FE로 리다이렉트. 예: "
                            + "http://localhost:5173/oauth/notion/callback?workspaceId=1&status=success"),
    })
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @Parameter(description = "Notion이 발급한 인가 코드")
            @RequestParam String code,
            @Parameter(description = "authorize 호출 시 실어 보낸 state (ws-{workspaceId})", example = "ws-1")
            @RequestParam String state
    ) {
        Long workspaceId = Long.parseLong(state.substring(STATE_PREFIX.length()));

        String status = "success";
        try {
            notionOAuthService.connect(workspaceId, code);
        } catch (RuntimeException ex) {
            log.error("Notion 콜백 처리 실패: workspaceId={}", workspaceId, ex);
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
}
