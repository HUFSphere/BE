package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.domain.WorkspaceMember;
import com.hufsphere.linkboard.dto.request.WorkspaceMemberAddRequest;
import com.hufsphere.linkboard.dto.response.WorkspaceMemberAddResponse;
import com.hufsphere.linkboard.exception.AlreadyWorkspaceMemberException;
import com.hufsphere.linkboard.exception.MemberInviteForbiddenException;
import com.hufsphere.linkboard.exception.WorkspaceOrUserNotFoundException;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.repository.WorkspaceMemberRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
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

    @Transactional
    public WorkspaceMemberAddResponse addMember(
            Long workspaceId,
            Long loginUserId,
            WorkspaceMemberAddRequest request
    ) {
        // 워크스페이스 존재 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->
                        new WorkspaceOrUserNotFoundException(
                                "워크스페이스 또는 사용자를 찾을 수 없습니다"
                        )
                );

        // 요청자가 해당 워크스페이스의 멤버인지 확인
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

        // leader만 멤버 추가 가능
        if (!"leader".equalsIgnoreCase(
                requesterMembership.getRole()
        )) {
            throw new MemberInviteForbiddenException(
                    "멤버 초대는 팀장만 가능합니다"
            );
        }

        // 추가 대상 사용자 존재 확인
        AppUser targetUser =
                appUserRepository.findById(request.userId())
                        .orElseThrow(() ->
                                new WorkspaceOrUserNotFoundException(
                                        "워크스페이스 또는 사용자를 찾을 수 없습니다"
                                )
                        );

        // 이미 워크스페이스에 속한 사용자인지 확인
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

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return DEFAULT_ROLE;
        }

        return role.toLowerCase();
    }
}