package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.request.LoginRequest;
import com.hufsphere.linkboard.dto.request.OAuthLoginRequest;
import com.hufsphere.linkboard.dto.request.SignupRequest;
import com.hufsphere.linkboard.dto.request.UpdateMyInfoRequest;
import com.hufsphere.linkboard.dto.response.LoginResponse;
import com.hufsphere.linkboard.dto.response.MyInfoResponse;
import com.hufsphere.linkboard.dto.response.OAuthLoginResponse;
import com.hufsphere.linkboard.dto.response.SignupResponse;
import com.hufsphere.linkboard.dto.response.UpdateMyInfoResponse;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.security.JwtProvider;
import com.hufsphere.linkboard.service.AuthService;
import com.hufsphere.linkboard.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;
    private final JwtProvider jwtProvider;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    @Operation(summary = "일반 회원가입", description = "아이디/비밀번호/이름으로 회원가입한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = SignupResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "아이디 중복",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "SIGNUP_SUCCESS",
                        "회원가입이 완료되었습니다",
                        response
                ));
    }

    @Operation(summary = "일반 로그인", description = "아이디/비밀번호로 로그인하고 액세스/리프레시 토큰을 발급한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수값 누락",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "LOGIN_SUCCESS",
                        "로그인되었습니다",
                        response
                )
        );
    }

    @Operation(summary = "소셜 로그인 시작", description = "지원하는 소셜 제공자의 로그인 페이지로 리다이렉트한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302",
                    description = "소셜 로그인 페이지로 리다이렉트"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "미지원 제공자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/oauth/{provider}/authorize")
    public ResponseEntity<Void> startOAuth(
            @Parameter(description = "소셜 제공자", example = "google")
            @PathVariable String provider
    ) {
        if (!provider.equalsIgnoreCase("google")) {
            throw new IllegalArgumentException(
                    "지원하지 않는 소셜 제공자입니다 (github, google, kakao)"
            );
        }

        String authorizationUrl = UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email")
                .build()
                .encode()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizationUrl))
                .build();
    }

    @Operation(
            summary = "OAuth 로그인/가입",
            description = "인가 코드로 소셜 로그인한다. 최초 로그인 시 자동으로 계정이 생성된다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인/가입 성공",
                    content = @Content(schema = @Schema(implementation = OAuthLoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "OAuth 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/oauth")
    public ResponseEntity<ApiResponse<OAuthLoginResponse>> loginOAuth(
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        OAuthLoginResponse response =
                oAuthService.loginOrSignup(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "LOGIN_SUCCESS",
                        "로그인되었습니다",
                        response
                )
        );
    }

    @Operation(
            summary = "내 정보 조회",
            description = "Access Token을 이용해 로그인한 사용자의 정보를 조회한다.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer Access Token",
                            required = true,
                            example = "Bearer eyJhbGciOi..."
                    )
            }
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        Long userId = extractUserId(authorization);

        MyInfoResponse response =
                authService.getMyInfo(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "ME_OK",
                        "내 정보 조회 성공",
                        response
                )
        );
    }

    @Operation(
            summary = "내 정보 수정",
            description = "로그인한 사용자의 표시명 또는 모국어를 수정한다.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer Access Token",
                            required = true,
                            example = "Bearer eyJhbGciOi..."
                    )
            }
    )
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UpdateMyInfoResponse>> updateMyInfo(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestBody UpdateMyInfoRequest request
    ) {
        Long userId = extractUserId(authorization);

        UpdateMyInfoResponse response =
                authService.updateMyInfo(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "ME_UPDATED",
                        "내 정보가 수정되었습니다",
                        response
                )
        );
    }

    @Operation(
            summary = "로그아웃",
            description = "Access Token을 확인한 후 로그아웃 처리한다.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer Access Token",
                            required = true,
                            example = "Bearer eyJhbGciOi..."
                    )
            }
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "LOGOUT_SUCCESS",
                                      "message": "로그아웃되었습니다",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "미인증",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-09T17:03:00.000+00:00",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "로그인이 필요합니다",
                                      "path": "/api/v1/auth/logout"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        extractUserId(authorization);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "LOGOUT_SUCCESS",
                        "로그아웃되었습니다",
                        null
                )
        );
    }

    private Long extractUserId(String authorization) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new InvalidCredentialsException(
                    "로그인이 필요합니다"
            );
        }

        String token = authorization.substring(7);

        try {
            return jwtProvider.getUserIdFromToken(token);
        } catch (Exception ex) {
            throw new InvalidCredentialsException(
                    "로그인이 필요합니다"
            );
        }
    }
}