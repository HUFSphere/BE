package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.domain.WorkspaceMember;
import com.hufsphere.linkboard.dto.request.WorkspaceMemberAddRequest;
import com.hufsphere.linkboard.dto.response.WorkspaceDetailResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceListResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceMemberAddResponse;
import com.hufsphere.linkboard.exception.AlreadyWorkspaceMemberException;
import com.hufsphere.linkboard.exception.MemberInviteForbiddenException;
import com.hufsphere.linkboard.exception.WorkspaceAccessDeniedException;
import com.hufsphere.linkboard.exception.WorkspaceDetailNotFoundException;
import com.hufsphere.linkboard.exception.WorkspaceOrUserNotFoundException;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import com.hufsphere.linkboard.repository.WorkspaceMemberRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceMemberService {

    private static final String DEFAULT_ROLE = "member";

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AppUserRepository appUserRepository;
    private final SourceConnectionRepository sourceConnectionRepository;

    @Transactional
    public WorkspaceMemberAddResponse addMember(
            Long workspaceId,
            Long loginUserId,
            WorkspaceMemberAddRequest request
    ) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->
                        new WorkspaceOrUserNotFoundException(
                                "워크스페이스 또는 사용자를 찾을 수 없습니다"
                        )
                );

        WorkspaceMember requesterMembership =
                workspaceMemberRepository
                        .findByWorkspaceIdAndUserId(
                                workspaceId,
                                loginUserId
                        )
                        .orElseThrow(() ->
                                new MemberInviteForbiddenException(
                                        "멤버 초대는 팀장만 가능합니다"
                                )
                        );

        if (!"leader".equalsIgnoreCase(
                requesterMembership.getRole()
        )) {
            throw new MemberInviteForbiddenException(
                    "멤버 초대는 팀장만 가능합니다"
            );
        }

        AppUser targetUser =
                appUserRepository.findById(request.userId())
                        .orElseThrow(() ->
                                new WorkspaceOrUserNotFoundException(
                                        "워크스페이스 또는 사용자를 찾을 수 없습니다"
                                )
                        );

        if (workspaceMemberRepository
                .existsByWorkspaceIdAndUserId(
                        workspaceId,
                        targetUser.getId()
                )) {
            throw new AlreadyWorkspaceMemberException(
                    "이미 워크스페이스에 속한 사용자입니다"
            );
        }

        String role = normalizeRole(
                request.role()
        );

        WorkspaceMember membership =
                WorkspaceMember.builder()
                        .workspace(workspace)
                        .user(targetUser)
                        .role(role)
                        .build();

        WorkspaceMember saved =
                workspaceMemberRepository.save(
                        membership
                );

        return WorkspaceMemberAddResponse.from(
                saved
        );
    }

    public List<WorkspaceListResponse> getMyWorkspaces(
            Long loginUserId
    ) {
        List<WorkspaceMember> memberships =
                workspaceMemberRepository
                        .findAllByUserId(loginUserId);

        return memberships.stream()
                .map(membership -> {
                    Workspace workspace =
                            membership.getWorkspace();

                    int sourceCount =
                            Math.toIntExact(
                                    sourceConnectionRepository
                                            .countByWorkspaceId(
                                                    workspace.getId()
                                            )
                            );

                    return new WorkspaceListResponse(
                            workspace.getId(),
                            workspace.getName(),
                            membership.getRole(),
                            workspace.getOwner().getId(),
                            sourceCount,
                            workspace.getCreatedAt()
                    );
                })
                .toList();
    }

    public WorkspaceDetailResponse getWorkspaceDetail(
            Long workspaceId,
            Long loginUserId
    ) {
        Workspace workspace =
                workspaceRepository.findById(workspaceId)
                        .orElseThrow(() ->
                                new WorkspaceDetailNotFoundException(
                                        "워크스페이스를 찾을 수 없습니다"
                                )
                        );

        WorkspaceMember membership =
                workspaceMemberRepository
                        .findByWorkspaceIdAndUserId(
                                workspaceId,
                                loginUserId
                        )
                        .orElseThrow(() ->
                                new WorkspaceAccessDeniedException(
                                        "이 워크스페이스에 접근 권한이 없습니다"
                                )
                        );

        int memberCount =
                Math.toIntExact(
                        workspaceMemberRepository
                                .countByWorkspaceId(
                                        workspaceId
                                )
                );

        int sourceCount =
                Math.toIntExact(
                        sourceConnectionRepository
                                .countByWorkspaceId(
                                        workspaceId
                                )
                );

        return new WorkspaceDetailResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getOwner().getId(),
                membership.getRole(),
                memberCount,
                sourceCount,
                workspace.getCreatedAt()
        );
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return DEFAULT_ROLE;
        }

        return role.toLowerCase();
    }
}