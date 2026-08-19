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
import java.util.HashMap;
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

    // 빈 페이지·한두 줄짜리 스텁 페이지는 work item으로서 의미가 없으므로 걸러낸다.
    // (임의 기준: 공백 제외 20자 미만이면 "내용 없음"으로 취급)
    private static final int MIN_NOTION_BODY_LENGTH = 20;

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
            IngestSignals signals = ingest(sourceConnection);
            syncWorkItems(sourceConnection, signals);
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

    // FIGMA/NOTION을 동기화한 경우에만 방금 크롤링한 완료 신호(코멘트 체크마크 / to_do 체크 비율)를
    // 돌려준다. 다른 소스를 동기화할 때는 해당 소스를 다시 크롤링하지 않으므로 빈 값을 돌려주는데,
    // replaceForWorkspace는 워크스페이스 전체 work item을 재생성하는 구조라 이 경우 기존
    // Figma/Notion work item도 AI가 새로 추측한 status로 재생성된다(신호 재적용 안 됨, 다음
    // 해당 소스 동기화 때 다시 반영됨).
    private record IngestSignals(List<FigmaComment> figmaComments, Map<String, Integer> notionCompletionByUrl) {
        static IngestSignals empty() {
            return new IngestSignals(List.of(), Map.of());
        }
    }

    private IngestSignals ingest(SourceConnection sourceConnection) {
        switch (sourceConnection.getSourceType()) {
            case GITHUB -> aiServerClient.ingestGithub(
                    sourceConnection.getSourceRef(), INGEST_MONTHS, resolveGithubAccessToken(sourceConnection));
            case NOTION -> {
                NotionCrawlResult crawled = crawlNotionPages(sourceConnection);
                aiServerClient.ingestNotion(crawled.pages());
                return new IngestSignals(List.of(), crawled.completionByUrl());
            }
            case FIGMA -> {
                List<FigmaComment> comments = crawlFigmaComments(sourceConnection);
                aiServerClient.ingestFigma(comments);
                return new IngestSignals(comments, Map.of());
            }
        }
        return IngestSignals.empty();
    }

    private String resolveGithubAccessToken(SourceConnection sourceConnection) {
        Long workspaceId = sourceConnection.getWorkspace().getId();
        GithubConnection githubConnection = githubConnectionRepository
                .findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> new GithubNotConnectedException("먼저 GitHub를 연결해주세요"));

        return githubConnection.getAccessToken();
    }

    private record NotionCrawlResult(List<NotionPage> pages, Map<String, Integer> completionByUrl) {}

    private NotionCrawlResult crawlNotionPages(SourceConnection sourceConnection) {
        Long workspaceId = sourceConnection.getWorkspace().getId();
        NotionConnection notionConnection = notionConnectionRepository
                .findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> new NotionNotConnectedException("먼저 Notion을 연결해주세요"));

        String accessToken = notionConnection.getAccessToken();
        Map<String, Integer> completionByUrl = new HashMap<>();

        List<NotionPage> pages = notionCrawlerClient.searchPages(accessToken).stream()
                // 제목 없는 페이지(빈 서브페이지, 제목 속성 없는 DB row 등)는
                // work item으로서 의미가 없으므로 AI로 보내기 전에 걸러낸다.
                .filter(page -> !NotionCrawlerClient.UNTITLED_PLACEHOLDER.equals(page.getTitle()))
                .map(page -> {
                    NotionCrawlerClient.NotionPageContent content =
                            notionCrawlerClient.fetchPageText(accessToken, page.getId());
                    // to_do 체크리스트가 있는 페이지만 완료율 신호로 취급한다(없으면 AI 추측 status 유지).
                    if (content.todoTotal() > 0) {
                        int rate = Math.round(100f * content.todoChecked() / content.todoTotal());
                        completionByUrl.put(page.getUrl(), rate);
                    }
                    return new NotionPage(page.getTitle(), page.getUrl(), content.text(), NOTION_DEFAULT_ITEM_TYPE);
                })
                // 본문이 비었거나 너무 짧은 페이지(빈 페이지, 제목만 있는 스텁 등)도 걸러낸다.
                .filter(notionPage -> notionPage.getText().trim().length() >= MIN_NOTION_BODY_LENGTH)
                .toList();

        return new NotionCrawlResult(pages, completionByUrl);
    }

    private List<FigmaComment> crawlFigmaComments(SourceConnection sourceConnection) {
        Long workspaceId = sourceConnection.getWorkspace().getId();
        FigmaConnection figmaConnection = figmaConnectionRepository
                .findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> new FigmaNotConnectedException("먼저 Figma를 연결해주세요"));

        return figmaCrawlerClient.fetchComments(figmaConnection.getAccessToken(), sourceConnection.getSourceRef());
    }

    private void syncWorkItems(SourceConnection sourceConnection, IngestSignals signals) {
        String lang = resolveLang(sourceConnection);

        ExtractWorkItemsResponse extracted = aiServerClient.extractWorkItems(lang);
        LinkWorkItemsResponse linked = aiServerClient.linkWorkItems(lang, LINK_TOP_K);

        // 코멘트 URL별 완료 여부. 같은 URL이 중복되는 경우는 없지만, 혹시 있다면 하나라도
        // 체크마크가 있으면 done으로 취급한다.
        Map<String, Boolean> figmaDoneByUrl = signals.figmaComments().stream()
                .collect(Collectors.toMap(FigmaComment::getUrl, FigmaComment::isDone, (a, b) -> a || b));

        workItemSyncService.replaceForWorkspace(
                sourceConnection.getWorkspace(), extracted.getWorkItems(), linked.getLinks(), lang,
                sourceConnection.getSourceType(), figmaDoneByUrl, signals.notionCompletionByUrl());
    }

    private String resolveLang(SourceConnection sourceConnection) {
        String defaultLanguage = sourceConnection.getWorkspace() != null
                ? sourceConnection.getWorkspace().getDefaultLanguage()
                : null;
        return defaultLanguage != null && !defaultLanguage.isBlank() ? defaultLanguage : DEFAULT_LANG;
    }
}
