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
import java.util.Map;
import java.util.stream.Collectors;
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
            List<FigmaComment> figmaComments = ingest(sourceConnection);
            syncWorkItems(sourceConnection, figmaComments);
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

    // FIGMA를 동기화한 경우에만 방금 크롤링한 코멘트를 돌려준다(체크마크 상태 반영용).
    // 다른 소스를 동기화할 때는 Figma를 다시 크롤링하지 않으므로 빈 리스트를 돌려주는데,
    // replaceForWorkspace는 워크스페이스 전체 work item을 재생성하는 구조라 이 경우
    // 기존 Figma work item도 AI가 새로 추측한 status로 재생성된다(체크마크 재적용 안 됨,
    // 다음 Figma 동기화 때 다시 반영됨).
    private List<FigmaComment> ingest(SourceConnection sourceConnection) {
        switch (sourceConnection.getSourceType()) {
            case GITHUB -> aiServerClient.ingestGithub(
                    sourceConnection.getSourceRef(), INGEST_MONTHS, resolveGithubAccessToken(sourceConnection));
            case NOTION -> aiServerClient.ingestNotion(crawlNotionPages(sourceConnection));
            case FIGMA -> {
                List<FigmaComment> comments = crawlFigmaComments(sourceConnection);
                aiServerClient.ingestFigma(comments);
                return comments;
            }
        }
        return List.of();
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
                // 제목 없는 페이지(빈 서브페이지, 제목 속성 없는 DB row 등)는
                // work item으로서 의미가 없으므로 AI로 보내기 전에 걸러낸다.
                .filter(page -> !NotionCrawlerClient.UNTITLED_PLACEHOLDER.equals(page.getTitle()))
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

    private void syncWorkItems(SourceConnection sourceConnection, List<FigmaComment> figmaComments) {
        String lang = resolveLang(sourceConnection);

        ExtractWorkItemsResponse extracted = aiServerClient.extractWorkItems(lang);
        LinkWorkItemsResponse linked = aiServerClient.linkWorkItems(lang, LINK_TOP_K);

        // 코멘트 URL별 완료 여부. 같은 URL이 중복되는 경우는 없지만, 혹시 있다면 하나라도
        // 체크마크가 있으면 done으로 취급한다.
        Map<String, Boolean> figmaDoneByUrl = figmaComments.stream()
                .collect(Collectors.toMap(FigmaComment::getUrl, FigmaComment::isDone, (a, b) -> a || b));

        workItemSyncService.replaceForWorkspace(
                sourceConnection.getWorkspace(), extracted.getWorkItems(), linked.getLinks(), lang, figmaDoneByUrl);
    }

    private String resolveLang(SourceConnection sourceConnection) {
        String defaultLanguage = sourceConnection.getWorkspace() != null
                ? sourceConnection.getWorkspace().getDefaultLanguage()
                : null;
        return defaultLanguage != null && !defaultLanguage.isBlank() ? defaultLanguage : DEFAULT_LANG;
    }
}
