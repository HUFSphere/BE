package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.service.FigmaOAuthService;
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
@Tag(name = "Figma OAuth", description = "Figma OAuth 연결 API")
@RestController
@RequestMapping("/api/v1/auth/figma")
@RequiredArgsConstructor
public class FigmaOAuthController {

    private static final String FIGMA_AUTHORIZE_URL = "https://www.figma.com/oauth";
    // Figma 스코프 이름은 "files:read"가 아니라 "file_content:read"이다.
    // (Figma OAuth 문서: https://developers.figma.com/docs/rest-api/oauth-apps/)
    // FigmaOAuthClient.fetchUser()가 호출하는 GET /v1/me는 current_user:read 스코프가 없으면 실패한다.
    private static final String FIGMA_SCOPE = "file_content:read,file_comments:read,current_user:read";
    private static final String STATE_PREFIX = "ws-";
    // FE가 이 경로에서 code/state 처리 결과를 받는다. status=success면 워크스페이스 설정 화면으로
    // 이어가고, status=error면 실패 안내를 띄우면 된다.
    private static final String CALLBACK_LANDING_PATH = "/oauth/figma/callback";

    private final FigmaOAuthService figmaOAuthService;

    @Value("${figma.client-id}")
    private String clientId;

    @Value("${figma.redirect-uri}")
    private String redirectUri;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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

    @Operation(
            summary = "Figma 연결 콜백",
            description = "Figma 인가 코드를 액세스 토큰으로 교환해 워크스페이스에 저장한 뒤, FE의 "
                    + CALLBACK_LANDING_PATH + " 로 리다이렉트한다(JSON을 직접 반환하지 않음). "
                    + "FE는 쿼리 파라미터 workspaceId, status(success|error)를 읽어 처리하면 된다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "처리 완료 후 FE로 리다이렉트. 예: "
                            + "http://localhost:5173/oauth/figma/callback?workspaceId=1&status=success"),
    })
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @Parameter(description = "Figma가 발급한 인가 코드. 사용자가 동의 화면에서 취소하면 없음")
            @RequestParam(required = false) String code,
            @Parameter(description = "authorize 호출 시 실어 보낸 state (ws-{workspaceId})", example = "ws-1")
            @RequestParam String state,
            @Parameter(description = "사용자가 취소했을 때 Figma가 대신 실어 보내는 값(예: access_denied)")
            @RequestParam(required = false) String error
    ) {
        Long workspaceId = Long.parseLong(state.substring(STATE_PREFIX.length()));

        String status;
        if (error != null || code == null || code.isBlank()) {
            // 사용자가 Figma 동의 화면에서 "취소"를 누른 경우. code 없이 error 파라미터만 온다.
            log.info("Figma 연결 취소/실패: workspaceId={}, error={}", workspaceId, error);
            status = "error";
        } else {
            status = "success";
            try {
                figmaOAuthService.connect(workspaceId, code);
            } catch (RuntimeException ex) {
                log.error("Figma 콜백 처리 실패: workspaceId={}", workspaceId, ex);
                status = "error";
            }
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
