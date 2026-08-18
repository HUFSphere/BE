package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.FigmaCrawlerClient;
import com.hufsphere.linkboard.client.NotionCrawlerClient;
import com.hufsphere.linkboard.client.dto.ExtractWorkItemsResponse;
import com.hufsphere.linkboard.client.dto.FigmaComment;
import com.hufsphere.linkboard.client.dto.LinkWorkItemsResponse;
import com.hufsphere.linkboard.client.dto.NotionPage;
import com.hufsphere.linkboard.domain.FigmaConnection;
import com.hufsphere.linkboard.domain.GithubConnection;
import com.hufsphere.linkboard.domain.NotionConnection;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.dto.response.SourceSyncResponse;
import com.hufsphere.linkboard.exception.FigmaNotConnectedException;
import com.hufsphere.linkboard.exception.GithubNotConnectedException;
import com.hufsphere.linkboard.exception.NotionNotConnectedException;
import com.hufsphere.linkboard.exception.SourceNotFoundException;
import com.hufsphere.linkboard.exception.SyncAlreadyRunningException;
import com.hufsphere.linkboard.repository.FigmaConnectionRepository;
import com.hufsphere.linkboard.repository.GithubConnectionRepository;
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
    private final FigmaConnectionRepository figmaConnectionRepository;
    private final GithubConnectionRepository githubConnectionRepository;
    private final AiServerClient aiServerClient;
    private final NotionCrawlerClient notionCrawlerClient;
    private final FigmaCrawlerClient figmaCrawlerClient;
    private final WorkItemSyncService workItemSyncService;

    public SourceSyncResponse sync(Long sourceId) {
        SourceConnection sourceConnection = sourceConnectionRepository.findById(sourceId)
                .orElseThrow(() -> new SourceNotFoundException("소스 연결을 찾을 수 없습니다"));

        if (sourceConnection.isSyncInProgress()) {
            throw new SyncAlreadyRunningException("이미 동기화가 진행 중입니다");
        }

        LocalDateTime startedAt = LocalDateTime.now();
        sourceConnection.startSyncing();
        sourceConnectionRepository.save(sourceConnection);

        try {
            ingest(sourceConnection);
            syncWorkItems(sourceConnection);
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

    private void ingest(SourceConnection sourceConnection) {
        switch (sourceConnection.getSourceType()) {
            case GITHUB -> aiServerClient.ingestGithub(
                    sourceConnection.getSourceRef(), INGEST_MONTHS, resolveGithubAccessToken(sourceConnection));
            case NOTION -> aiServerClient.ingestNotion(crawlNotionPages(sourceConnection));
            case FIGMA -> aiServerClient.ingestFigma(crawlFigmaComments(sourceConnection));
        }
    }

    private String resolveGithubAccessToken(SourceConnection sourceConnection) {
        Long workspaceId = sourceConnection.getWorkspace().getId();
        GithubConnection githubConnection = githubConnectionRepository
                .findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> new GithubNotConnectedException("먼저 GitHub를 연결해주세요"));

        return githubConnection.getAccessToken();
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

    private List<FigmaComment> crawlFigmaComments(SourceConnection sourceConnection) {
        Long workspaceId = sourceConnection.getWorkspace().getId();
        FigmaConnection figmaConnection = figmaConnectionRepository
                .findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> new FigmaNotConnectedException("먼저 Figma를 연결해주세요"));

        return figmaCrawlerClient.fetchComments(figmaConnection.getAccessToken(), sourceConnection.getSourceRef());
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
}
