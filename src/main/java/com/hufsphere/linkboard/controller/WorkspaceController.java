package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.dto.WorkspaceSettingResponse;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateRequest;
import com.hufsphere.linkboard.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Workspace", description = "워크스페이스 설정 및 관리 API")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "워크스페이스 설정 조회 (7.1)", description = "워크스페이스의 기본 정보 및 설정값을 조회합니다.")
    @GetMapping("/{workspaceId}/settings")
    public ResponseEntity<?> getWorkspaceSettings(@PathVariable Long workspaceId) {
        WorkspaceSettingResponse response = workspaceService.getWorkspaceSettings(workspaceId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "WORKSPACE_SETTING_OK",
                "message", "워크스페이스 설정 조회 성공",
                "data", response
        ));
    }

    @Operation(summary = "워크스페이스 설정 수정 (7.2)", description = "워크스페이스의 이름, 설명, 기본 언어 등 설정을 수정합니다.")
    @PutMapping("/{workspaceId}/settings")
    public ResponseEntity<?> updateWorkspaceSettings(
            @PathVariable Long workspaceId,
            @RequestBody WorkspaceUpdateRequest request
    ) {
        WorkspaceSettingResponse response = workspaceService.updateWorkspaceSettings(workspaceId, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "WORKSPACE_UPDATED",
                "message", "워크스페이스 설정 수정 성공",
                "data", response
        ));
    }
}