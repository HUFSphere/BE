package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceSettingResponse;
import com.hufsphere.linkboard.dto.request.WorkspaceCreateRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceMemberAddRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateNameRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateRequest;
import com.hufsphere.linkboard.dto.response.WorkspaceCreateResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceDetailResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceListResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceMemberAddResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceUpdateNameResponse;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.security.JwtProvider;
import com.hufsphere.linkboard.service.WorkspaceMemberService;
import com.hufsphere.linkboard.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Workspace",
        description = "워크스페이스 설정 및 관리 API"
)
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceMemberService workspaceMemberService;
    private final JwtProvider jwtProvider;

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

    @Operation(
            summary = "멤버 초대/추가",
            description = "팀장이 사용자를 워크스페이스 멤버로 추가합니다."
    )
    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<ApiResponse<WorkspaceMemberAddResponse>>
    addMember(
            @PathVariable Long workspaceId,

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
            WorkspaceMemberAddRequest request
    ) {
        Long loginUserId =
                extractUserId(authorization);

        WorkspaceMemberAddResponse response =
                workspaceMemberService.addMember(
                        workspaceId,
                        loginUserId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "MEMBER_ADDED",
                                "멤버가 추가되었습니다",
                                response
                        )
                );
    }

    @Operation(
            summary = "내 워크스페이스 목록",
            description = "로그인한 사용자가 속한 워크스페이스 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceListResponse>>>
    getMyWorkspaces(
            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization
    ) {
        Long loginUserId =
                extractUserId(authorization);

        List<WorkspaceListResponse> response =
                workspaceMemberService
                        .getMyWorkspaces(
                                loginUserId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "WORKSPACES_OK",
                        "워크스페이스 목록 조회 성공",
                        response
                )
        );
    }

    @Operation(
            summary = "워크스페이스 상세 조회",
            description = "로그인한 사용자가 속한 워크스페이스의 상세 정보를 조회합니다."
    )
    @GetMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceDetailResponse>>
    getWorkspaceDetail(
            @PathVariable Long workspaceId,

            @Parameter(
                    description = "Bearer Access Token",
                    required = true,
                    example = "Bearer eyJ..."
            )
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization
    ) {
        Long loginUserId =
                extractUserId(authorization);

        WorkspaceDetailResponse response =
                workspaceMemberService
                        .getWorkspaceDetail(
                                workspaceId,
                                loginUserId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "WORKSPACE_OK",
                        "워크스페이스 조회 성공",
                        response
                )
        );
    }

    @Operation(
            summary = "워크스페이스 수정",
            description = "팀장이 워크스페이스 이름을 수정합니다."
    )
    @PatchMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<WorkspaceUpdateNameResponse>>
    updateWorkspace(
            @PathVariable Long workspaceId,

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
            WorkspaceUpdateNameRequest request
    ) {
        Long loginUserId =
                extractUserId(authorization);

        WorkspaceUpdateNameResponse response =
                workspaceMemberService.updateWorkspaceName(
                        workspaceId,
                        loginUserId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "WORKSPACE_UPDATED",
                        "워크스페이스가 수정되었습니다",
                        response
                )
        );
    }

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
                workspaceService
                        .updateWorkspaceSettings(
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