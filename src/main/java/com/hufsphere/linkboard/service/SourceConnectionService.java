package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.dto.SourceConnectionResponse;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SourceConnectionService {

    private final SourceConnectionRepository sourceConnectionRepository;

    public List<SourceConnectionResponse> getSourceConnections(Long workspaceId) {
        List<SourceConnection> connections = sourceConnectionRepository.findByWorkspaceId(workspaceId);

        return connections.stream()
                .map(conn -> SourceConnectionResponse.builder()
                        .id(conn.getId())
                        .workspaceId(workspaceId)
                        .sourceType(conn.getSourceType() != null ? conn.getSourceType().name().toLowerCase() : null)
                        .status(conn.getStatus() != null ? conn.getStatus().name().toLowerCase() : null)
                        .targetRepoOrBoard(conn.getTargetRepoOrBoard())
                        .lastSyncedAt(conn.getLastSyncedAt())
                        .createdAt(conn.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}