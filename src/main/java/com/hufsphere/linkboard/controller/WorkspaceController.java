package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.client.dto.WorkItemResponseDto;
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

import java.util.List;
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
            summary = "작업 목록 조회 (5.4)",
            description = "워크스페이스 내 작업 목록을 조회합니다. platform(GITHUB, FIGMA, NOTION)과 status로 필터링할 수 있습니다."
    )
    @GetMapping("/{workspaceId}/work-items")
    public ResponseEntity<?> getWorkItems(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String status
    ) {
        List<WorkItemResponseDto> response = workspaceService.getWorkItems(workspaceId, platform, status);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "WORK_ITEMS_OK",
                "message", "작업 목록 조회 성공",
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