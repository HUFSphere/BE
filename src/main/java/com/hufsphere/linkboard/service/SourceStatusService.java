package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.dto.response.SourceStatusResponse;
import com.hufsphere.linkboard.exception.SourceNotFoundException;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SourceStatusService {

    private final SourceConnectionRepository sourceConnectionRepository;
    private final WorkItemRepository workItemRepository;

    @Transactional(readOnly = true)
    public SourceStatusResponse getStatus(Long sourceId) {
        SourceConnection sourceConnection = sourceConnectionRepository.findById(sourceId)
                .orElseThrow(() -> new SourceNotFoundException("소스 연결을 찾을 수 없습니다"));

        int indexedCount = (int) workItemRepository.countBySourceConnectionId(sourceId);
        return SourceStatusResponse.of(sourceConnection, indexedCount);
    }
}
