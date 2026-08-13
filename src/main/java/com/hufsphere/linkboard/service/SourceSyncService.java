package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.dto.ExtractWorkItemsResponse;
import com.hufsphere.linkboard.client.dto.FigmaComment;
import com.hufsphere.linkboard.client.dto.LinkWorkItemsResponse;
import com.hufsphere.linkboard.client.dto.NotionPage;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.dto.request.FigmaCommentRequest;
import com.hufsphere.linkboard.dto.request.NotionPageRequest;
import com.hufsphere.linkboard.dto.request.SourceSyncRequest;
import com.hufsphere.linkboard.dto.response.SourceSyncResponse;
import com.hufsphere.linkboard.exception.InvalidSyncPayloadException;
import com.hufsphere.linkboard.exception.SourceFetchFailedException;
import com.hufsphere.linkboard.exception.SourceNotFoundException;
import com.hufsphere.linkboard.exception.SyncAlreadyRunningException;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SourceSyncService {

    private static final int INGEST_MONTHS = 3;
    private static final int LINK_TOP_K = 4;
    private static final String DEFAULT_LANG = "ko";

    private final SourceConnectionRepository sourceConnectionRepository;
    private final AiServerClient aiServerClient;
    private final WorkItemSyncService workItemSyncService;

    public SourceSyncResponse sync(Long sourceId, SourceSyncRequest request) {
        SourceConnection sourceConnection = sourceConnectionRepository.findById(sourceId)
                .orElseThrow(() -> new SourceNotFoundException("소스 연결을 찾을 수 없습니다"));

        if (sourceConnection.isSyncInProgress()) {
            throw new SyncAlreadyRunningException("이미 동기화가 진행 중입니다");
        }

        validatePayload(sourceConnection, request);

        LocalDateTime startedAt = LocalDateTime.now();
        sourceConnection.startSyncing();
        sourceConnectionRepository.save(sourceConnection);

        try {
            ingest(sourceConnection, request);
            if (sourceConnection.getSourceType() == SourceType.GITHUB) {
                syncWorkItems(sourceConnection);
            }
        } catch (SourceFetchFailedException e) {
            sourceConnection.failSyncing();
            sourceConnectionRepository.save(sourceConnection);
            throw e;
        }

        sourceConnection.completeSyncing(LocalDateTime.now());
        sourceConnectionRepository.save(sourceConnection);

        return SourceSyncResponse.of(sourceConnection, startedAt);
    }

    private void validatePayload(SourceConnection sourceConnection, SourceSyncRequest request) {
        switch (sourceConnection.getSourceType()) {
            case NOTION -> {
                if (request == null || request.getPages() == null || request.getPages().isEmpty()) {
                    throw new InvalidSyncPayloadException("notion 동기화에는 pages가 필요합니다");
                }
            }
            case FIGMA -> {
                if (request == null || request.getComments() == null || request.getComments().isEmpty()) {
                    throw new InvalidSyncPayloadException("figma 동기화에는 comments가 필요합니다");
                }
            }
            case GITHUB -> {
            }
        }
    }

    private void ingest(SourceConnection sourceConnection, SourceSyncRequest request) {
        switch (sourceConnection.getSourceType()) {
            case GITHUB -> aiServerClient.ingestGithub(sourceConnection.getSourceRef(), INGEST_MONTHS);
            case NOTION -> aiServerClient.ingestNotion(toNotionPages(request.getPages()));
            case FIGMA -> aiServerClient.ingestFigma(toFigmaComments(request.getComments()));
        }
    }

    private void syncWorkItems(SourceConnection sourceConnection) {
        String lang = resolveLang(sourceConnection);

        ExtractWorkItemsResponse extracted = aiServerClient.extractWorkItems(lang);
        LinkWorkItemsResponse linked = aiServerClient.linkWorkItems(lang, LINK_TOP_K);

        workItemSyncService.replace(sourceConnection, extracted.getWorkItems(), linked.getLinks());
    }

    private String resolveLang(SourceConnection sourceConnection) {
        String defaultLanguage = sourceConnection.getWorkspace() != null
                ? sourceConnection.getWorkspace().getDefaultLanguage()
                : null;
        return defaultLanguage != null && !defaultLanguage.isBlank() ? defaultLanguage : DEFAULT_LANG;
    }

    private List<NotionPage> toNotionPages(List<NotionPageRequest> pages) {
        return pages.stream()
                .map(page -> new NotionPage(page.getTitle(), page.getUrl(), page.getText(), page.getItemType()))
                .toList();
    }

    private List<FigmaComment> toFigmaComments(List<FigmaCommentRequest> comments) {
        return comments.stream()
                .map(comment -> new FigmaComment(comment.getFrameName(), comment.getUrl(), comment.getText()))
                .toList();
    }
}
