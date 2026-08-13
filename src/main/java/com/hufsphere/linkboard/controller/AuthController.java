package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.request.LoginRequest;
import com.hufsphere.linkboard.dto.request.SignupRequest;
import com.hufsphere.linkboard.dto.response.LoginResponse;
import com.hufsphere.linkboard.dto.response.SignupResponse;
import com.hufsphere.linkboard.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "일반 회원가입", description = "아이디/비밀번호/이름으로 회원가입한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = SignupResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SIGNUP_SUCCESS",
                                      "message": "회원가입이 완료되었습니다",
                                      "data": {
                                        "userId": 1,
                                        "username": "jaeyoung123",
                                        "name": "박재영",
                                        "nativeLang": "ko",
                                        "createdAt": "2026-08-12T10:00:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-12T10:00:00.000+00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "비밀번호는 8자 이상이어야 합니다",
                                      "path": "/api/v1/auth/signup"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "아이디 중복",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-12T10:00:00.000+00:00",
                                      "status": 409,
                                      "error": "Conflict",
                                      "message": "이미 사용 중인 아이디입니다",
                                      "path": "/api/v1/auth/signup"
                                    }"""))),
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("SIGNUP_SUCCESS", "회원가입이 완료되었습니다", response));
    }

    @Operation(summary = "일반 로그인", description = "아이디/비밀번호로 로그인하고 액세스/리프레시 토큰을 발급한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "LOGIN_SUCCESS",
                                      "message": "로그인되었습니다",
                                      "data": {
                                        "userId": 1,
                                        "username": "jaeyoung123",
                                        "name": "박재영",
                                        "nativeLang": "ko",
                                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6..."
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수값 누락",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-12T10:05:00.000+00:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "username과 password는 필수입니다",
                                      "path": "/api/v1/auth/login"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-12T10:05:00.000+00:00",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "아이디 또는 비밀번호가 올바르지 않습니다",
                                      "path": "/api/v1/auth/login"
                                    }"""))),
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("LOGIN_SUCCESS", "로그인되었습니다", response));
    }
}
