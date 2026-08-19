package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.dto.WorkItemResponseDto;
import com.hufsphere.linkboard.client.dto.WorkspaceInviteResponse;
import com.hufsphere.linkboard.common.FigmaUrlParser;
import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.domain.WorkspaceMember;
import com.hufsphere.linkboard.dto.*;
import com.hufsphere.linkboard.dto.request.WorkspaceCreateRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateRequest;
import com.hufsphere.linkboard.dto.response.WorkspaceCreateResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceSettingResponse;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import com.hufsphere.linkboard.repository.WorkspaceMemberRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private static final String CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 헷갈리는 I, O, 0, 1 제외 난수 풀
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WorkspaceRepository workspaceRepository;
    private final WorkItemRepository workItemRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AppUserRepository appUserRepository;
    private final SourceConnectionRepository sourceConnectionRepository;

    /*
     * 2.1 워크스페이스 생성
     */
    @Transactional
    public WorkspaceCreateResponse createWorkspace(
            Long loginUserId,
            WorkspaceCreateRequest request
    ) {
        AppUser creator = appUserRepository.findById(loginUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다"
                        )
                );

        LocalDateTime now = LocalDateTime.now();

        String inviteCode = generateInviteCode();

        Workspace workspace = Workspace.builder()
                .name(request.name())
                .defaultLanguage("ko")
                .owner(creator)
                .inviteCode(inviteCode)
                .inviteCodeExpiresAt(now.plusDays(7))
                .createdAt(now)
                .updatedAt(now)
                .build();

        Workspace savedWorkspace =
                workspaceRepository.save(workspace);

        /*
         * 워크스페이스 생성자를 자동으로 leader 등록
         */
        WorkspaceMember leader = WorkspaceMember.builder()
                .workspace(savedWorkspace)
                .user(creator)
                .role("leader")
                .build();

        workspaceMemberRepository.save(leader);

        /*
         * 선택적으로 입력된 소스만 PENDING 상태로 생성
         */
        List<WorkspaceCreateResponse.SourceInfo> sourceResponses =
                new ArrayList<>();

        if (hasText(request.githubRepo())) {
            SourceConnection github =
                    createPendingSource(
                            savedWorkspace,
                            SourceType.GITHUB,
                            request.githubRepo()
                    );

            sourceResponses.add(
                    toSourceInfo(github)
            );
        }

        if (hasText(request.notionUrl())) {
            SourceConnection notion =
                    createPendingSource(
                            savedWorkspace,
                            SourceType.NOTION,
                            request.notionUrl()
                    );

            sourceResponses.add(
                    toSourceInfo(notion)
            );
        }

        if (hasText(request.figmaUrl())) {
            SourceConnection figma =
                    createPendingSource(
                            savedWorkspace,
                            SourceType.FIGMA,
                            request.figmaUrl()
                    );

            sourceResponses.add(
                    toSourceInfo(figma)
            );
        }

        return new WorkspaceCreateResponse(
                savedWorkspace.getId(),
                savedWorkspace.getName(),
                creator.getId(),
                savedWorkspace.getInviteCode(),
                savedWorkspace.getInviteCodeExpiresAt(),
                sourceResponses,
                savedWorkspace.getCreatedAt()
        );
    }

    private SourceConnection createPendingSource(
            Workspace workspace,
            SourceType sourceType,
            String sourceRef
    ) {
        // Figma는 사용자가 공유 링크를 그대로 붙여넣으므로, API 호출에 쓰이는 순수 fileKey로 정규화한다.
        String targetRepoOrBoard = sourceType == SourceType.FIGMA
                ? FigmaUrlParser.extractFileKey(sourceRef)
                : sourceRef;

        SourceConnection connection =
                SourceConnection.builder()
                        .workspace(workspace)
                        .sourceType(sourceType)
                        .targetRepoOrBoard(targetRepoOrBoard)
                        .status("PENDING")
                        .build();

        return sourceConnectionRepository.save(connection);
    }

    private WorkspaceCreateResponse.SourceInfo toSourceInfo(
            SourceConnection connection
    ) {
        return new WorkspaceCreateResponse.SourceInfo(
                connection.getId(),
                connection.getSourceType().getValue(),
                connection.getSourceRef(),
                connection.getStatus().toLowerCase()
        );
    }

    private boolean hasText(String value) {
        return value != null
                && !value.isBlank();
    }

    /*
     * 기존 7.1 워크스페이스 설정 조회
     */
    public WorkspaceSettingResponse getWorkspaceSettings(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId));

        return WorkspaceSettingResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .defaultLanguage(
                        workspace.getDefaultLanguage() != null
                                ? workspace.getDefaultLanguage()
                                : "ko"
                )
                .updatedAt(
                        workspace.getUpdatedAt() != null
                                ? workspace.getUpdatedAt()
                                : LocalDateTime.now()
                )
                .build();
    }

    /*
     * 기존 7.2 워크스페이스 설정 수정
     */
    @Transactional
    public WorkspaceSettingResponse updateWorkspaceSettings(Long workspaceId, WorkspaceUpdateRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId));

        if (request.getName() != null
                && !request.getName().isBlank()) {
            workspace.setName(
                    request.getName()
            );
        }

        if (request.getDescription() != null) {
            workspace.setDescription(
                    request.getDescription()
            );
        }

        if (request.getDefaultLanguage() != null
                && !request.getDefaultLanguage().isBlank()) {
            workspace.setDefaultLanguage(
                    request.getDefaultLanguage()
            );
        }

        workspace.setUpdatedAt(
                LocalDateTime.now()
        );

        return WorkspaceSettingResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .defaultLanguage(
                        workspace.getDefaultLanguage()
                )
                .updatedAt(
                        workspace.getUpdatedAt()
                )
                .build();
    }

    // 5.4 작업 목록 조회 (플랫폼 및 상태 필터링)
    public List<WorkItemResponseDto> getWorkItems(Long workspaceId, String platform, String status) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId);
        }

        String filteredPlatform = (platform != null && !platform.trim().isEmpty()) ? platform.trim() : null;
        String filteredStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;

        List<WorkItem> workItems = workItemRepository.findByWorkspaceIdAndFilters(workspaceId, filteredPlatform, filteredStatus);

        return workItems.stream()
                .map(WorkItemResponseDto::from)
                .toList();
    }

    // [신규] 초대 코드 발급 및 재발급 (7일 만료)
    @Transactional
    public WorkspaceInviteResponse generateOrRenewInviteCode(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId));

        String inviteCode = generateInviteCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        workspace.updateInviteCode(inviteCode, expiresAt);

        return WorkspaceInviteResponse.builder()
                .workspaceId(workspace.getId())
                .inviteCode(inviteCode)
                .expiresAt(expiresAt)
                .build();
    }

    // 5.6 대시보드 AI 추천 질문
    public SuggestedQuestionsResponse getSuggestedQuestions(Long workspaceId, String lang) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId);
        }

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

    // 8자리 난수 코드 생성 유틸 (예: XK79-2M9Q)
    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append("-");
            }
            int randomIndex = RANDOM.nextInt(CODE_CHARACTERS.length());
            sb.append(CODE_CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
}
