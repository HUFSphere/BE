package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.dto.WorkspaceSettingResponse;
import com.hufsphere.linkboard.dto.request.WorkspaceCreateRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateRequest;
import com.hufsphere.linkboard.dto.response.WorkspaceCreateResponse;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.security.JwtProvider;
import com.hufsphere.linkboard.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Workspace",
        description = "워크스페이스 설정 및 관리 API"
)
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final JwtProvider jwtProvider;

    /*
     * 2.1 워크스페이스 생성
     */
    @Operation(
            summary = "워크스페이스 생성",
            description = "워크스페이스를 생성하고 생성자를 leader로 등록합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceCreateResponse>>
    createWorkspace(
            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,

            @Valid
            @RequestBody
            WorkspaceCreateRequest request
    ) {
        Long loginUserId =
                extractUserId(authorization);

        WorkspaceCreateResponse response =
                workspaceService.createWorkspace(
                        loginUserId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "WORKSPACE_CREATED",
                                "워크스페이스가 생성되었습니다",
                                response
                        )
                );
    }

    /*
     * 기존 7.1
     */
    @Operation(
            summary = "워크스페이스 설정 조회 (7.1)",
            description = "워크스페이스의 기본 정보 및 설정값을 조회합니다."
    )
    @GetMapping("/{workspaceId}/settings")
    public ResponseEntity<?> getWorkspaceSettings(
            @PathVariable Long workspaceId
    ) {
        WorkspaceSettingResponse response =
                workspaceService.getWorkspaceSettings(
                        workspaceId
                );

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "code", "WORKSPACE_SETTING_OK",
                        "message", "워크스페이스 설정 조회 성공",
                        "data", response
                )
        );
    }

    /*
     * 기존 7.2
     */
    @Operation(
            summary = "워크스페이스 설정 수정 (7.2)",
            description = "워크스페이스의 이름, 설명, 기본 언어 등 설정을 수정합니다."
    )
    @PutMapping("/{workspaceId}/settings")
    public ResponseEntity<?> updateWorkspaceSettings(
            @PathVariable Long workspaceId,
            @RequestBody WorkspaceUpdateRequest request
    ) {
        WorkspaceSettingResponse response =
                workspaceService.updateWorkspaceSettings(
                        workspaceId,
                        request
                );

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "code", "WORKSPACE_UPDATED",
                        "message", "워크스페이스 설정 수정 성공",
                        "data", response
                )
        );
    }

    private Long extractUserId(
            String authorization
    ) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new InvalidCredentialsException(
                    "로그인이 필요합니다"
            );
        }

        String token =
                authorization.substring(7);

        try {
            return jwtProvider.getUserIdFromToken(
                    token
            );
        } catch (Exception ex) {
            throw new InvalidCredentialsException(
                    "로그인이 필요합니다"
            );
        }
    }
}