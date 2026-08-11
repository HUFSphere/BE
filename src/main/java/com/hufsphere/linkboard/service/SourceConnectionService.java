package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.dto.request.SourceConnectionCreateRequest;
import com.hufsphere.linkboard.dto.response.SourceConnectionResponse;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SourceConnectionService {

    private final WorkspaceRepository workspaceRepository;
    private final SourceConnectionRepository sourceConnectionRepository;

    @Transactional
    public SourceConnectionResponse connectSource(Long workspaceId, SourceConnectionCreateRequest request) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없거나 소스에 접근할 수 없습니다");
        }

        SourceType sourceType = SourceType.fromValue(request.getSourceType());

        SourceConnection sourceConnection = SourceConnection.builder()
                .workspaceId(workspaceId)
                .sourceType(sourceType)
                .sourceRef(request.getSourceRef())
                .build();

        SourceConnection saved = sourceConnectionRepository.save(sourceConnection);
        return SourceConnectionResponse.from(saved);
    }
}
