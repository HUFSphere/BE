package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.domain.WorkspaceMember;
import com.hufsphere.linkboard.dto.WorkspaceSettingResponse;
import com.hufsphere.linkboard.dto.request.WorkspaceCreateRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateRequest;
import com.hufsphere.linkboard.dto.response.WorkspaceCreateResponse;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
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

    private static final String INVITE_CODE_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private final WorkspaceRepository workspaceRepository;
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
        SourceConnection connection =
                SourceConnection.builder()
                        .workspace(workspace)
                        .sourceType(sourceType)
                        .targetRepoOrBoard(sourceRef)
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

    private String generateInviteCode() {
        return randomPart(4)
                + "-"
                + randomPart(4);
    }

    private String randomPart(int length) {
        StringBuilder result =
                new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index =
                    RANDOM.nextInt(
                            INVITE_CODE_CHARS.length()
                    );

            result.append(
                    INVITE_CODE_CHARS.charAt(index)
            );
        }

        return result.toString();
    }

    private boolean hasText(String value) {
        return value != null
                && !value.isBlank();
    }

    /*
     * 기존 7.1 워크스페이스 설정 조회
     */
    public WorkspaceSettingResponse getWorkspaceSettings(
            Long workspaceId
    ) {
        Workspace workspace =
                workspaceRepository.findById(workspaceId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "워크스페이스를 찾을 수 없습니다. id="
                                                + workspaceId
                                )
                        );

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
    public WorkspaceSettingResponse updateWorkspaceSettings(
            Long workspaceId,
            WorkspaceUpdateRequest request
    ) {
        Workspace workspace =
                workspaceRepository.findById(workspaceId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "워크스페이스를 찾을 수 없습니다. id="
                                                + workspaceId
                                )
                        );

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
}