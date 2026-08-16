package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.domain.WorkspaceMember;
import com.hufsphere.linkboard.dto.request.SourceConnectionCreateRequest;
import com.hufsphere.linkboard.dto.response.SourceConnectionResponse;
import com.hufsphere.linkboard.dto.response.SourceSyncResponse;
import com.hufsphere.linkboard.exception.SourceDeleteForbiddenException;
import com.hufsphere.linkboard.exception.SourceNotFoundException;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import com.hufsphere.linkboard.repository.WorkItemLinkRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import com.hufsphere.linkboard.repository.WorkspaceMemberRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SourceConnectionService {

    private final SourceConnectionRepository sourceConnectionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkItemRepository workItemRepository;
    private final WorkItemLinkRepository workItemLinkRepository;

    @Transactional
    public SourceConnectionResponse createConnection(Long workspaceId, SourceConnectionCreateRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 워크스페이스입니다."));

        SourceConnection connection = SourceConnection.builder()
                .workspace(workspace)
                .sourceType(SourceType.fromValue(request.getSourceType()))
                .targetRepoOrBoard(request.getTargetRepoOrBoard())
                .status("CONNECTED")
                .build();

        SourceConnection saved = sourceConnectionRepository.save(connection);
        return SourceConnectionResponse.of(saved);
    }

    public List<SourceConnectionResponse> getConnections(Long workspaceId) {
        return sourceConnectionRepository.findByWorkspaceId(workspaceId).stream()
                .map(SourceConnectionResponse::of)
                .collect(Collectors.toList());
    }

    // 3.5 소스 연결 해제
    @Transactional
    public void deleteConnection(Long sourceId, Long loginUserId) {
        SourceConnection connection = sourceConnectionRepository.findById(sourceId)
                .orElseThrow(() -> new SourceNotFoundException("소스 연결을 찾을 수 없습니다"));

        Long workspaceId = connection.getWorkspace().getId();

        WorkspaceMember membership = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, loginUserId)
                .orElseThrow(() -> new SourceDeleteForbiddenException("소스 연결 해제는 팀장만 가능합니다"));

        if (!"leader".equalsIgnoreCase(membership.getRole())) {
            throw new SourceDeleteForbiddenException("소스 연결 해제는 팀장만 가능합니다");
        }

        workItemLinkRepository.deleteBySourceConnectionId(sourceId);
        workItemRepository.deleteBySourceConnectionId(sourceId);
        sourceConnectionRepository.delete(connection);
    }
}