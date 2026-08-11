package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.WorkItemLink;
import com.hufsphere.linkboard.dto.WorkItemDetailResponse;
import com.hufsphere.linkboard.dto.WorkItemPageResponse;
import com.hufsphere.linkboard.dto.WorkItemSummaryResponse;
import com.hufsphere.linkboard.repository.WorkItemLinkRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
        // 1. workspaceId 기준 조건 검색 (기본 필터링 적용)
        List<WorkItem> workItems;
        if (sourceType != null && !sourceType.isBlank()) {
            SourceType type = SourceType.valueOf(sourceType.toLowerCase());
            workItems = workItemRepository.findByWorkspaceIdAndSourceType(workspaceId, type);
        } else {
            workItems = workItemRepository.findByWorkspaceId(workspaceId);
        }

        // 2. 검색어(query) 및 상태(status) 메모리 필터링 (간이 구현)
        List<WorkItem> filteredItems = workItems.stream()
                .filter(item -> query == null || query.isBlank() || item.getTitle().contains(query))
                .filter(item -> status == null || status.isBlank() || status.equalsIgnoreCase(item.getStatus()))
                .collect(Collectors.toList());

        // 3. DTO 변환
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

        // 4. 페이징 계산 및 반환
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
}