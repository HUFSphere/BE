package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.common.WorkItemStatusLabels;
import com.hufsphere.linkboard.domain.Feature;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.WorkItemLink;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.dto.DashboardFeaturesResponse;
import com.hufsphere.linkboard.dto.DashboardSourcesResponse;
import com.hufsphere.linkboard.dto.TeamDashboardResponse;
import com.hufsphere.linkboard.dto.WorkItemDetailResponse;
import com.hufsphere.linkboard.dto.WorkItemPageResponse;
import com.hufsphere.linkboard.dto.WorkItemSummaryResponse;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.FeatureRepository;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import com.hufsphere.linkboard.repository.WorkItemLinkRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkItemService {

    private static final int TOP_FEATURES = 3;
    private static final int RECENT_ISSUES_PER_SOURCE = 3;
    private static final String DONE_STATUS = "done";

    private final WorkItemRepository workItemRepository;
    private final WorkItemLinkRepository workItemLinkRepository;
    private final WorkspaceRepository workspaceRepository;
    private final FeatureRepository featureRepository;
    private final SourceConnectionRepository sourceConnectionRepository;

    private static String resolveLang(String lang, Workspace workspace) {
        if (lang != null && !lang.isBlank()) {
            return lang;
        }
        String defaultLanguage = workspace != null ? workspace.getDefaultLanguage() : null;
        return defaultLanguage != null && !defaultLanguage.isBlank() ? defaultLanguage : "ko";
    }

    public WorkItemDetailResponse getWorkItemDetail(Long workItemId, String lang) {
        WorkItem workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. id=" + workItemId));

        String resolvedLang = resolveLang(lang, workItem.getWorkspace());

        List<WorkItemLink> links = workItemLinkRepository.findByFromWorkItemIdOrToWorkItemId(workItemId, workItemId);

        List<WorkItemDetailResponse.LinkedItemResponse> linkedItems = links.stream()
                .map(link -> {
                    WorkItem target = link.getFromWorkItem().getId().equals(workItemId)
                            ? link.getToWorkItem()
                            : link.getFromWorkItem();

                    return WorkItemDetailResponse.LinkedItemResponse.builder()
                            .id(target.getId())
                            .sourceType(target.getSourceType() != null ? target.getSourceType().name().toLowerCase() : null)
                            .itemType(target.getItemType())
                            .title(target.getTitle())
                            .sourceUrl(target.getSourceUrl())
                            .linkReason(link.getLinkReason())
                            .build();
                })
                .collect(Collectors.toList());

        return WorkItemDetailResponse.builder()
                .id(workItem.getId())
                .sourceType(workItem.getSourceType() != null ? workItem.getSourceType().name().toLowerCase() : null)
                .itemType(workItem.getItemType())
                .sourceNumber(workItem.getSourceNumber())
                .title(workItem.getTitle())
                .status(workItem.getStatus())
                .statusLabel(WorkItemStatusLabels.resolve(workItem.getStatus(), resolvedLang))
                .completionRate(workItem.getCompletionRate())
                .authorLogin(workItem.getAuthorLogin())
                .sourceUrl(workItem.getSourceUrl())
                .sourceUpdatedAt(workItem.getSourceUpdatedAt())
                .summaryNative(workItem.getSummaryNative())
                .linkedItems(linkedItems)
                .build();
    }

    public WorkItemSummaryResponse getWorkItemSummary(Long workItemId, String lang) {
        WorkItem workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. id=" + workItemId));

        String summaryText = workItem.getSummaryNative() != null && !workItem.getSummaryNative().isBlank()
                ? workItem.getSummaryNative()
                : workItem.getTitle() + " 에 대한 " + lang + " 언어 요약입니다.";

        return WorkItemSummaryResponse.builder()
                .workItemId(workItem.getId())
                .lang(lang)
                .summaryText(summaryText)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public WorkItemPageResponse getWorkItems(
            Long workspaceId,
            String query,
            String sourceType,
            String status,
            String sort,
            int page,
            int size,
            String lang
    ) {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        String resolvedLang = resolveLang(lang, workspace);

        List<WorkItem> workItems;
        if (sourceType != null && !sourceType.isBlank()) {
            SourceType type = SourceType.fromValue(sourceType);
            workItems = workItemRepository.findByWorkspaceIdAndSourceTypeOrderBySourceUpdatedAtDesc(workspaceId, type);
        } else {
            workItems = workItemRepository.findByWorkspaceIdOrderBySourceUpdatedAtDesc(workspaceId);
        }

        List<WorkItem> filteredItems = workItems.stream()
                .filter(item -> query == null || query.isBlank() || item.getTitle().contains(query))
                .filter(item -> status == null || status.isBlank() || status.equalsIgnoreCase(item.getStatus()))
                .collect(Collectors.toList());

        List<WorkItemPageResponse.ItemSummary> items = filteredItems.stream()
                .map(item -> WorkItemPageResponse.ItemSummary.builder()
                        .id(item.getId())
                        .sourceType(item.getSourceType() != null ? item.getSourceType().name().toLowerCase() : null)
                        .itemType(item.getItemType())
                        .sourceNumber(item.getSourceNumber())
                        .title(item.getTitle())
                        .status(item.getStatus())
                        .statusLabel(WorkItemStatusLabels.resolve(item.getStatus(), resolvedLang))
                        .completionRate(item.getCompletionRate())
                        .authorLogin(item.getAuthorLogin())
                        .sourceUrl(item.getSourceUrl())
                        .sourceUpdatedAt(item.getSourceUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return WorkItemPageResponse.builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    public TeamDashboardResponse getTeamDashboard(Long workspaceId) {
        List<WorkItem> workItems = workItemRepository.findByWorkspaceIdOrderBySourceUpdatedAtDesc(workspaceId);

        // 1. 전체 작업 상태별 통계 집계
        Map<String, Integer> globalStatusCounts = new HashMap<>();
        for (WorkItem item : workItems) {
            String st = item.getStatus() != null ? item.getStatus() : "UNKNOWN";
            globalStatusCounts.put(st, globalStatusCounts.getOrDefault(st, 0) + 1);
        }

        // 2. 작성자(팀원)별 작업 그룹화 및 집계
        Map<String, List<WorkItem>> groupedByAuthor = workItems.stream()
                .filter(item -> item.getAuthorLogin() != null)
                .collect(Collectors.groupingBy(WorkItem::getAuthorLogin));

        List<TeamDashboardResponse.MemberStatus> memberStatuses = groupedByAuthor.entrySet().stream()
                .map(entry -> {
                    String author = entry.getKey();
                    List<WorkItem> items = entry.getValue();

                    Map<String, Integer> mCounts = new HashMap<>();
                    for (WorkItem i : items) {
                        String st = i.getStatus() != null ? i.getStatus() : "UNKNOWN";
                        mCounts.put(st, mCounts.getOrDefault(st, 0) + 1);
                    }

                    return TeamDashboardResponse.MemberStatus.builder()
                            .authorLogin(author)
                            .totalAssigned(items.size())
                            .statusCounts(mCounts)
                            .build();
                })
                .collect(Collectors.toList());

        return TeamDashboardResponse.builder()
                .totalWorkItems(workItems.size())
                .statusCounts(globalStatusCounts)
                .members(memberStatuses)
                .build();
    }

    public DashboardFeaturesResponse getFeatureProgress(Long workspaceId) {
        requireWorkspace(workspaceId);

        List<Feature> features = featureRepository.findByWorkspaceId(workspaceId);
        Map<Long, List<WorkItem>> itemsByFeatureId = workItemRepository.findByWorkspaceIdOrderBySourceUpdatedAtDesc(workspaceId).stream()
                .filter(item -> item.getFeatureId() != null)
                .collect(Collectors.groupingBy(WorkItem::getFeatureId));

        List<DashboardFeaturesResponse.FeatureProgress> progresses = features.stream()
                .map(feature -> toFeatureProgress(feature, itemsByFeatureId.getOrDefault(feature.getId(), List.of())))
                .sorted(Comparator.comparing(
                        DashboardFeaturesResponse.FeatureProgress::getLastUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(TOP_FEATURES)
                .collect(Collectors.toList());

        return DashboardFeaturesResponse.builder().features(progresses).build();
    }

    private DashboardFeaturesResponse.FeatureProgress toFeatureProgress(Feature feature, List<WorkItem> items) {
        int total = items.size();
        int done = (int) items.stream().filter(item -> DONE_STATUS.equals(item.getStatus())).count();
        LocalDateTime lastUpdatedAt = items.stream()
                .map(WorkItem::getSourceUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return DashboardFeaturesResponse.FeatureProgress.builder()
                .featureId(feature.getId())
                .name(feature.getName())
                .totalCount(total)
                .doneCount(done)
                .progress(calculateProgress(done, total))
                .lastUpdatedAt(lastUpdatedAt)
                .build();
    }

    public DashboardSourcesResponse getSourceProgress(Long workspaceId) {
        requireWorkspace(workspaceId);

        List<SourceConnection> connections = sourceConnectionRepository.findByWorkspaceId(workspaceId);
        Map<Long, List<WorkItem>> itemsBySourceConnectionId = workItemRepository.findByWorkspaceIdOrderBySourceUpdatedAtDesc(workspaceId).stream()
                .filter(item -> item.getSourceConnection() != null)
                .collect(Collectors.groupingBy(item -> item.getSourceConnection().getId()));

        List<DashboardSourcesResponse.SourceCard> cards = connections.stream()
                .map(connection -> toSourceCard(connection, itemsBySourceConnectionId.getOrDefault(connection.getId(), List.of())))
                .collect(Collectors.toList());

        return DashboardSourcesResponse.builder().sources(cards).build();
    }

    private DashboardSourcesResponse.SourceCard toSourceCard(SourceConnection connection, List<WorkItem> items) {
        int total = items.size();
        int done = (int) items.stream().filter(item -> DONE_STATUS.equals(item.getStatus())).count();

        List<DashboardSourcesResponse.RecentIssue> recentIssues = items.stream()
                .sorted(Comparator.comparing(WorkItem::getSourceUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_ISSUES_PER_SOURCE)
                .map(item -> DashboardSourcesResponse.RecentIssue.builder()
                        .workItemId(item.getId())
                        .title(item.getTitle())
                        .status(item.getStatus())
                        .sourceUrl(item.getSourceUrl())
                        .sourceUpdatedAt(item.getSourceUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return DashboardSourcesResponse.SourceCard.builder()
                .sourceId(connection.getId())
                .sourceType(connection.getSourceType() != null ? connection.getSourceType().getValue() : null)
                .sourceRef(connection.getTargetRepoOrBoard())
                .connStatus(connection.getStatus())
                .totalCount(total)
                .doneCount(done)
                .progress(calculateProgress(done, total))
                .recentIssues(recentIssues)
                .build();
    }

    private double calculateProgress(int done, int total) {
        return total == 0 ? 0.0 : (double) done / total;
    }

    private void requireWorkspace(Long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다");
        }
    }
}