package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.dto.response.SourceStatusResponse;
import com.hufsphere.linkboard.exception.SourceNotFoundException;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SourceStatusService {

    // TODO: work_item 엔티티 도입 후 실제 인덱싱된 아이템 개수로 교체
    private static final int INDEXED_COUNT_PLACEHOLDER = 0;

    private final SourceConnectionRepository sourceConnectionRepository;

    @Transactional(readOnly = true)
    public SourceStatusResponse getStatus(Long sourceId) {
        SourceConnection sourceConnection = sourceConnectionRepository.findById(sourceId)
                .orElseThrow(() -> new SourceNotFoundException("소스 연결을 찾을 수 없습니다"));

        return SourceStatusResponse.of(sourceConnection, INDEXED_COUNT_PLACEHOLDER);
    }
}
