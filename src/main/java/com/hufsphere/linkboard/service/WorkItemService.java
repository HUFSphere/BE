package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.WorkItemLink;
import com.hufsphere.linkboard.dto.TeamDashboardResponse;
import com.hufsphere.linkboard.dto.WorkItemDetailResponse;
import com.hufsphere.linkboard.dto.WorkItemPageResponse;
import com.hufsphere.linkboard.dto.WorkItemSummaryResponse;
import com.hufsphere.linkboard.repository.WorkItemLinkRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final WorkItemLinkRepository workItemLinkRepository;

    public WorkItemDetailResponse getWorkItemDetail(Long workItemId, String lang) {
        WorkItem workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. id=" + workItemId));

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
            int size
    ) {
        List<WorkItem> workItems;
        if (sourceType != null && !sourceType.isBlank()) {
            SourceType type = SourceType.fromValue(sourceType);
            workItems = workItemRepository.findByWorkspaceIdAndSourceType(workspaceId, type);
        } else {
            workItems = workItemRepository.findByWorkspaceId(workspaceId);
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
        List<WorkItem> workItems = workItemRepository.findByWorkspaceId(workspaceId);

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
}