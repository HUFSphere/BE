package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.dto.RecentActivitiesResponse;
import com.hufsphere.linkboard.dto.RecentActivityDto;
import com.hufsphere.linkboard.dto.SuggestedQuestionsResponse;
import com.hufsphere.linkboard.dto.WorkspaceSettingResponse;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateRequest;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkItemRepository workItemRepository;

    public WorkspaceSettingResponse getWorkspaceSettings(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId));

        return WorkspaceSettingResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .defaultLanguage(workspace.getDefaultLanguage() != null ? workspace.getDefaultLanguage() : "ko")
                .updatedAt(workspace.getUpdatedAt() != null ? workspace.getUpdatedAt() : LocalDateTime.now())
                .build();
    }

    @Transactional
    public WorkspaceSettingResponse updateWorkspaceSettings(Long workspaceId, WorkspaceUpdateRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId));

        if (request.getName() != null && !request.getName().isBlank()) {
            workspace.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workspace.setDescription(request.getDescription());
        }
        if (request.getDefaultLanguage() != null && !request.getDefaultLanguage().isBlank()) {
            workspace.setDefaultLanguage(request.getDefaultLanguage());
        }
        workspace.setUpdatedAt(LocalDateTime.now());

        return WorkspaceSettingResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .defaultLanguage(workspace.getDefaultLanguage())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }

    // 5.6 대시보드 AI 추천 질문
    public SuggestedQuestionsResponse getSuggestedQuestions(Long workspaceId, String lang) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId);
        }

        // DB 저장이 필요 없는 조회 요청이므로 명세서 예시 응답 반환
        return new SuggestedQuestionsResponse(List.of(
                "이 프로젝트에서 인증 방식은 왜 JWT로 정해졌나요?",
                "최근에 가장 많이 논의된 기능은 무엇인가요?",
                "디자인 관련 결정 중 아직 개발에 반영 안 된 게 있나요?"
        ));
    }

    // 5.7 대시보드 최근 활동
    public RecentActivitiesResponse getRecentActivities(Long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId);
        }

        List<WorkItem> recentItems = workItemRepository.findTop3ByWorkspaceIdOrderBySourceUpdatedAtDesc(workspaceId);

        List<RecentActivityDto> activities = recentItems.stream()
                .map(item -> new RecentActivityDto(
                        item.getId(),
                        String.valueOf(item.getSourceType()),
                        String.valueOf(item.getItemType()),
                        item.getTitle(),
                        String.valueOf(item.getStatus()),
                        item.getSourceUrl(),
                        item.getSourceUpdatedAt()
                ))
                .toList();

        return new RecentActivitiesResponse(activities);
    }
}