package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.NotionCrawlerClient;
import com.hufsphere.linkboard.client.dto.ExtractWorkItemsResponse;
import com.hufsphere.linkboard.client.dto.FigmaComment;
import com.hufsphere.linkboard.client.dto.LinkWorkItemsResponse;
import com.hufsphere.linkboard.client.dto.NotionPage;
import com.hufsphere.linkboard.domain.NotionConnection;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.dto.request.FigmaCommentRequest;
import com.hufsphere.linkboard.dto.request.SourceSyncRequest;
import com.hufsphere.linkboard.dto.response.SourceSyncResponse;
import com.hufsphere.linkboard.exception.InvalidSyncPayloadException;
import com.hufsphere.linkboard.exception.NotionNotConnectedException;
import com.hufsphere.linkboard.exception.SourceFetchFailedException;
import com.hufsphere.linkboard.exception.SourceNotFoundException;
import com.hufsphere.linkboard.exception.SyncAlreadyRunningException;
import com.hufsphere.linkboard.repository.NotionConnectionRepository;
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
    private static final String NOTION_DEFAULT_ITEM_TYPE = "meeting";

    private final SourceConnectionRepository sourceConnectionRepository;
    private final NotionConnectionRepository notionConnectionRepository;
    private final AiServerClient aiServerClient;
    private final NotionCrawlerClient notionCrawlerClient;
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
            if (sourceConnection.getSourceType() == SourceType.GITHUB
                    || sourceConnection.getSourceType() == SourceType.NOTION) {
                syncWorkItems(sourceConnection);
            }
        } catch (Exception e) {
            // 어떤 예외든(예상 못한 NPE·파싱 오류 포함) SYNCING에 영구히 멈추지 않도록
            // 상태를 failed로 되돌린 뒤 원래 예외를 그대로 다시 던진다 (응답 계약은 그대로 유지).
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
            case FIGMA -> {
                if (request == null || request.getComments() == null || request.getComments().isEmpty()) {
                    throw new InvalidSyncPayloadException("figma 동기화에는 comments가 필요합니다");
                }
            }
            case GITHUB, NOTION -> {
            }
        }
    }

    private void ingest(SourceConnection sourceConnection, SourceSyncRequest request) {
        switch (sourceConnection.getSourceType()) {
            case GITHUB -> aiServerClient.ingestGithub(sourceConnection.getSourceRef(), INGEST_MONTHS);
            case NOTION -> aiServerClient.ingestNotion(crawlNotionPages(sourceConnection));
            case FIGMA -> aiServerClient.ingestFigma(toFigmaComments(request.getComments()));
        }
    }

    private List<NotionPage> crawlNotionPages(SourceConnection sourceConnection) {
        Long workspaceId = sourceConnection.getWorkspace().getId();
        NotionConnection notionConnection = notionConnectionRepository
                .findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> new NotionNotConnectedException("먼저 Notion을 연결해주세요"));

        String accessToken = notionConnection.getAccessToken();
        return notionCrawlerClient.searchPages(accessToken).stream()
                .map(page -> new NotionPage(
                        page.getTitle(),
                        page.getUrl(),
                        notionCrawlerClient.fetchPageText(accessToken, page.getId()),
                        NOTION_DEFAULT_ITEM_TYPE))
                .toList();
    }

    private void syncWorkItems(SourceConnection sourceConnection) {
        String lang = resolveLang(sourceConnection);

        ExtractWorkItemsResponse extracted = aiServerClient.extractWorkItems(lang);
        LinkWorkItemsResponse linked = aiServerClient.linkWorkItems(lang, LINK_TOP_K);

        workItemSyncService.replaceForWorkspace(sourceConnection.getWorkspace(), extracted.getWorkItems(), linked.getLinks());
    }

    private String resolveLang(SourceConnection sourceConnection) {
        String defaultLanguage = sourceConnection.getWorkspace() != null
                ? sourceConnection.getWorkspace().getDefaultLanguage()
                : null;
        return defaultLanguage != null && !defaultLanguage.isBlank() ? defaultLanguage : DEFAULT_LANG;
    }

    private List<FigmaComment> toFigmaComments(List<FigmaCommentRequest> comments) {
        return comments.stream()
                .map(comment -> new FigmaComment(comment.getFrameName(), comment.getUrl(), comment.getText()))
                .toList();
    }
}
