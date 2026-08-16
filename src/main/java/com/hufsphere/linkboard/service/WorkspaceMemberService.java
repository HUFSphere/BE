package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.domain.WorkspaceMember;
import com.hufsphere.linkboard.dto.request.WorkspaceJoinRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceMemberAddRequest;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateNameRequest;
import com.hufsphere.linkboard.dto.response.WorkspaceDetailResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceJoinResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceListResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceMemberAddResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceMemberResponse;
import com.hufsphere.linkboard.dto.response.WorkspaceUpdateNameResponse;
import com.hufsphere.linkboard.exception.AlreadyWorkspaceMemberException;
import com.hufsphere.linkboard.exception.InviteCodeNotFoundException;
import com.hufsphere.linkboard.exception.MemberInviteForbiddenException;
import com.hufsphere.linkboard.exception.MemberRemoveForbiddenException;
import com.hufsphere.linkboard.exception.WorkspaceAccessDeniedException;
import com.hufsphere.linkboard.exception.WorkspaceDeleteForbiddenException;
import com.hufsphere.linkboard.exception.WorkspaceDetailNotFoundException;
import com.hufsphere.linkboard.exception.WorkspaceLeaveForbiddenException;
import com.hufsphere.linkboard.exception.WorkspaceOrUserNotFoundException;
import com.hufsphere.linkboard.exception.WorkspaceUpdateForbiddenException;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import com.hufsphere.linkboard.repository.WorkItemLinkRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import com.hufsphere.linkboard.repository.WorkspaceMemberRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import java.time.LocalDateTime;
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
    private final WorkItemRepository workItemRepository;
    private final WorkItemLinkRepository workItemLinkRepository;

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

    @Transactional
    public WorkspaceUpdateNameResponse updateWorkspaceName(
            Long workspaceId,
            Long loginUserId,
            WorkspaceUpdateNameRequest request
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
                                new WorkspaceUpdateForbiddenException(
                                        "워크스페이스 수정은 팀장만 가능합니다"
                                )
                        );

        if (!"leader".equalsIgnoreCase(
                membership.getRole()
        )) {
            throw new WorkspaceUpdateForbiddenException(
                    "워크스페이스 수정은 팀장만 가능합니다"
            );
        }

        workspace.setName(
                request.name()
        );

        return new WorkspaceUpdateNameResponse(
                workspace.getId(),
                workspace.getName()
        );
    }

    @Transactional
    public void deleteWorkspace(
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
                                new WorkspaceDeleteForbiddenException(
                                        "워크스페이스 삭제는 팀장만 가능합니다"
                                )
                        );

        if (!"leader".equalsIgnoreCase(
                membership.getRole()
        )) {
            throw new WorkspaceDeleteForbiddenException(
                    "워크스페이스 삭제는 팀장만 가능합니다"
            );
        }

        workItemLinkRepository
                .deleteByWorkspaceId(workspaceId);

        workItemRepository
                .deleteByWorkspaceId(workspaceId);

        sourceConnectionRepository
                .deleteAllByWorkspaceId(workspaceId);

        workspaceMemberRepository
                .deleteAllByWorkspaceId(workspaceId);

        workspaceRepository.delete(workspace);
    }

    // 2.7 멤버 목록 조회
    public List<WorkspaceMemberResponse> getMembers(Long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceDetailNotFoundException(
                    "워크스페이스를 찾을 수 없습니다"
            );
        }

        return workspaceMemberRepository
                .findAllByWorkspaceId(workspaceId)
                .stream()
                .map(WorkspaceMemberResponse::from)
                .toList();
    }

    // 2.9 멤버 내보내기
    @Transactional
    public void removeMember(
            Long workspaceId,
            Long loginUserId,
            Long targetUserId
    ) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceDetailNotFoundException(
                    "워크스페이스를 찾을 수 없습니다"
            );
        }

        WorkspaceMember requesterMembership =
                workspaceMemberRepository
                        .findByWorkspaceIdAndUserId(
                                workspaceId,
                                loginUserId
                        )
                        .orElseThrow(() ->
                                new MemberRemoveForbiddenException(
                                        "멤버 내보내기는 팀장만 가능합니다"
                                )
                        );

        if (!"leader".equalsIgnoreCase(
                requesterMembership.getRole()
        )) {
            throw new MemberRemoveForbiddenException(
                    "멤버 내보내기는 팀장만 가능합니다"
            );
        }

        if (loginUserId.equals(targetUserId)) {
            throw new MemberRemoveForbiddenException(
                    "팀장 자신은 내보낼 수 없습니다. 먼저 팀장을 위임해주세요"
            );
        }

        WorkspaceMember targetMembership =
                workspaceMemberRepository
                        .findByWorkspaceIdAndUserId(
                                workspaceId,
                                targetUserId
                        )
                        .orElseThrow(() ->
                                new WorkspaceOrUserNotFoundException(
                                        "워크스페이스 또는 사용자를 찾을 수 없습니다"
                                )
                        );

        workspaceMemberRepository.delete(targetMembership);
    }

    // 2.10 워크스페이스 나가기
    @Transactional
    public void leaveWorkspace(
            Long workspaceId,
            Long loginUserId
    ) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceDetailNotFoundException(
                    "워크스페이스를 찾을 수 없습니다"
            );
        }

        WorkspaceMember membership =
                workspaceMemberRepository
                        .findByWorkspaceIdAndUserId(
                                workspaceId,
                                loginUserId
                        )
                        .orElseThrow(() ->
                                new WorkspaceAccessDeniedException(
                                        "이 워크스페이스의 멤버가 아닙니다"
                                )
                        );

        if ("leader".equalsIgnoreCase(
                membership.getRole()
        )) {
            throw new WorkspaceLeaveForbiddenException(
                    "팀장은 워크스페이스를 나갈 수 없습니다. 먼저 팀장을 위임해주세요"
            );
        }

        workspaceMemberRepository.delete(membership);
    }

    // 2.12 초대 코드로 참여
    @Transactional
    public WorkspaceJoinResponse joinWorkspace(
            Long loginUserId,
            WorkspaceJoinRequest request
    ) {
        Workspace workspace =
                workspaceRepository
                        .findByInviteCode(request.inviteCode())
                        .orElseThrow(() ->
                                new InviteCodeNotFoundException(
                                        "유효하지 않거나 만료된 초대 코드입니다"
                                )
                        );

        if (workspace.getInviteCodeExpiresAt() == null
                || workspace.getInviteCodeExpiresAt()
                        .isBefore(LocalDateTime.now())) {
            throw new InviteCodeNotFoundException(
                    "유효하지 않거나 만료된 초대 코드입니다"
            );
        }

        if (workspaceMemberRepository
                .existsByWorkspaceIdAndUserId(
                        workspace.getId(),
                        loginUserId
                )) {
            throw new AlreadyWorkspaceMemberException(
                    "이미 워크스페이스에 속해 있습니다"
            );
        }

        AppUser user =
                appUserRepository.findById(loginUserId)
                        .orElseThrow(() ->
                                new WorkspaceOrUserNotFoundException(
                                        "워크스페이스 또는 사용자를 찾을 수 없습니다"
                                )
                        );

        WorkspaceMember membership =
                WorkspaceMember.builder()
                        .workspace(workspace)
                        .user(user)
                        .role(DEFAULT_ROLE)
                        .build();

        workspaceMemberRepository.save(membership);

        return new WorkspaceJoinResponse(
                workspace.getId(),
                workspace.getName(),
                DEFAULT_ROLE
        );
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return DEFAULT_ROLE;
        }

        return role.toLowerCase();
    }
}