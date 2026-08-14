package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.client.dto.WorkspaceInviteResponse;
import com.hufsphere.linkboard.dto.RecentActivitiesResponse;
import com.hufsphere.linkboard.dto.SuggestedQuestionsResponse;
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

    @Operation(
            summary = "팀원 초대 코드 발급 및 재발급",
            description = "7일간 유효한 난수 초대 코드를 발급하거나 기존 코드를 재발급하여 갱신합니다."
    )
    @PostMapping("/{workspaceId}/invitations")
    public ResponseEntity<?> generateInviteCode(@PathVariable Long workspaceId) {
        WorkspaceInviteResponse response = workspaceService.generateOrRenewInviteCode(workspaceId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "INVITATION_CODE_CREATED",
                "message", "초대 코드 발급 성공",
                "data", response
        ));
    }

    @Operation(summary = "대시보드 AI 추천 질문 조회 (5.6)", description = "워크스페이스의 작업들을 바탕으로 생성된 AI 추천 질문 3개를 조회합니다.")
    @GetMapping("/{workspaceId}/suggested-questions")
    public ResponseEntity<?> getSuggestedQuestions(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) String lang
    ) {
        SuggestedQuestionsResponse response = workspaceService.getSuggestedQuestions(workspaceId, lang);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "SUGGESTED_QUESTIONS_OK",
                "message", "추천 질문 조회 성공",
                "data", response
        ));
    }

    @Operation(summary = "대시보드 최근 활동 조회 (5.7)", description = "가장 최근에 갱신된 작업 3개를 조회합니다.")
    @GetMapping("/{workspaceId}/recent-activities")
    public ResponseEntity<?> getRecentActivities(@PathVariable Long workspaceId) {
        RecentActivitiesResponse response = workspaceService.getRecentActivities(workspaceId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "RECENT_ACTIVITIES_OK",
                "message", "최근 활동 조회 성공",
                "data", response
        ));
    }
}