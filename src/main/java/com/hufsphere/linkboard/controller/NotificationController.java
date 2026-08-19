package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.response.NotificationListResponse;
import com.hufsphere.linkboard.dto.response.NotificationResponse;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.security.JwtProvider;
import com.hufsphere.linkboard.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final int DEFAULT_LIMIT = 20;

    private final NotificationService notificationService;
    private final JwtProvider jwtProvider;

    @Operation(
            summary = "알림 목록 조회",
            description = "요청자에게 온 알림을 최근순으로 최대 limit개 반환한다(워크스페이스와 무관하게 사용자 개인 단위로 모은다). "
                    + "알림 창에 최근 3~4개만 띄우려면 limit=4로 호출하면 된다. unreadCount는 limit과 무관하게 전체 안읽음 개수다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "알림 목록 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "NOTIFICATIONS_OK",
                              "message": "알림 목록 조회 성공",
                              "data": {
                                "notifications": [
                                  {
                                    "id": 12,
                                    "type": "SOURCE_SYNC_SUCCESS",
                                    "workspaceId": 1,
                                    "message": "github 동기화가 완료되었습니다",
                                    "read": false,
                                    "createdAt": "2026-08-20T10:00:00"
                                  }
                                ],
                                "unreadCount": 3
                              }
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인이 필요함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-20T10:00:00",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "로그인이 필요합니다",
                                      "path": "/api/v1/notifications"
                                    }"""))),
    })
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @Parameter(description = "Bearer Access Token", required = true, example = "Bearer eyJ...")
            @RequestHeader(value = "Authorization", required = false)
            String authorization,

            @Parameter(description = "최대 조회 개수", example = "20")
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT)
            int limit
    ) {
        Long userId = extractUserId(authorization);

        NotificationListResponse response = notificationService.getNotifications(userId, limit);

        return ResponseEntity.ok(ApiResponse.success("NOTIFICATIONS_OK", "알림 목록 조회 성공", response));
    }

    @Operation(
            summary = "알림 읽음 처리",
            description = "알림 하나를 읽음 처리한다. 요청자 본인의 알림이 아니면 404로 응답한다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "읽음 처리 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "NOTIFICATION_READ",
                              "message": "알림을 읽음 처리했습니다",
                              "data": {
                                "id": 12,
                                "type": "SOURCE_SYNC_SUCCESS",
                                "workspaceId": 1,
                                "message": "github 동기화가 완료되었습니다",
                                "read": true,
                                "createdAt": "2026-08-20T10:00:00"
                              }
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인이 필요함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "알림을 찾을 수 없음(본인 알림이 아닌 경우 포함)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @Parameter(description = "Bearer Access Token", required = true, example = "Bearer eyJ...")
            @RequestHeader(value = "Authorization", required = false)
            String authorization,

            @Parameter(description = "알림 ID", example = "12")
            @PathVariable Long id
    ) {
        Long userId = extractUserId(authorization);

        NotificationResponse response = notificationService.markAsRead(userId, id);

        return ResponseEntity.ok(ApiResponse.success("NOTIFICATION_READ", "알림을 읽음 처리했습니다", response));
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
