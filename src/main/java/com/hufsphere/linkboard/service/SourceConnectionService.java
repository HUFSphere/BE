package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.dto.request.SourceConnectionCreateRequest;
import com.hufsphere.linkboard.dto.response.SourceConnectionResponse;
import com.hufsphere.linkboard.dto.response.SourceSyncResponse;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
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
}