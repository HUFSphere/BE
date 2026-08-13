package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.dto.response.SourceSyncResponse;
import com.hufsphere.linkboard.exception.SourceNotFoundException;
import com.hufsphere.linkboard.exception.SyncAlreadyRunningException;
import com.hufsphere.linkboard.exception.UnsupportedSourceSyncException;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SourceSyncService {

    private final SourceConnectionRepository sourceConnectionRepository;
    private final AiServerClient aiServerClient;

    @Transactional
    public SourceSyncResponse sync(Long sourceId) {
        SourceConnection sourceConnection = sourceConnectionRepository.findById(sourceId)
                .orElseThrow(() -> new SourceNotFoundException("소스 연결을 찾을 수 없습니다"));

        if (sourceConnection.isSyncInProgress()) {
            throw new SyncAlreadyRunningException("이미 동기화가 진행 중입니다");
        }

        if (sourceConnection.getSourceType() != SourceType.GITHUB) {
            throw new UnsupportedSourceSyncException("현재는 github 소스만 동기화를 지원합니다");
        }

        LocalDateTime startedAt = LocalDateTime.now();
        sourceConnection.startSyncing();
        sourceConnectionRepository.save(sourceConnection);

        try {
            aiServerClient.triggerSync(sourceId, sourceConnection.getSourceRef());
            sourceConnection.completeSyncing(startedAt);
        } catch (Exception e) {
            sourceConnection.failSyncing();
            throw e;
        } finally {
            sourceConnectionRepository.save(sourceConnection);
        }

        return SourceSyncResponse.of(sourceConnection, startedAt);
    }
}