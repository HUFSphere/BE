package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.dto.SourceConnectionResponse;
import com.hufsphere.linkboard.dto.SourceSyncResponse;
import com.hufsphere.linkboard.dto.request.SourceConnectionCreateRequest;
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

    public List<SourceConnectionResponse> getSourceConnections(Long workspaceId) {
        List<SourceConnection> connections = sourceConnectionRepository.findByWorkspaceId(workspaceId);

        return connections.stream()
                .map(conn -> SourceConnectionResponse.builder()
                        .id(conn.getId())
                        .workspaceId(workspaceId)
                        .sourceType(conn.getSourceType() != null ? conn.getSourceType().name().toLowerCase() : null)
                        .status(conn.getStatus())
                        .targetRepoOrBoard(conn.getTargetRepoOrBoard())
                        .lastSyncedAt(conn.getLastSyncedAt())
                        .createdAt(conn.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public SourceConnectionResponse createSourceConnection(Long workspaceId, SourceConnectionCreateRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId));

        SourceConnection connection = SourceConnection.builder()
                .workspace(workspace)
                .sourceType(SourceType.valueOf(request.getSourceType().toUpperCase()))
                .targetRepoOrBoard(request.getTargetRepoOrBoard())
                .status("CONNECTED")
                .build();

        SourceConnection saved = sourceConnectionRepository.save(connection);

        return SourceConnectionResponse.builder()
                .id(saved.getId())
                .workspaceId(workspaceId)
                .sourceType(saved.getSourceType() != null ? saved.getSourceType().name().toLowerCase() : null)
                .status(saved.getStatus())
                .targetRepoOrBoard(saved.getTargetRepoOrBoard())
                .lastSyncedAt(saved.getLastSyncedAt())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public SourceSyncResponse triggerSync(Long connectionId) {
        SourceConnection connection = sourceConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("연동 출처를 찾을 수 없습니다. id=" + connectionId));

        connection.setLastSyncedAt(LocalDateTime.now());
        connection.setStatus("SYNCED");

        return SourceSyncResponse.builder()
                .connectionId(connection.getId())
                .status("IN_PROGRESS")
                .message("동기화 작업이 시작되었습니다.")
                .syncedAt(connection.getLastSyncedAt())
                .build();
    }

    public SourceSyncResponse getConnectionStatus(Long connectionId) {
        SourceConnection connection = sourceConnectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("연동 출처를 찾을 수 없습니다. id=" + connectionId));

        return SourceSyncResponse.builder()
                .connectionId(connection.getId())
                .status(connection.getStatus() != null ? connection.getStatus() : "CONNECTED")
                .message("정상 연동 상태입니다.")
                .syncedAt(connection.getLastSyncedAt())
                .build();
    }
}