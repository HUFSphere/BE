package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.WorkItemLink;
import com.hufsphere.linkboard.dto.WorkItemDetailResponse;
import com.hufsphere.linkboard.repository.WorkItemLinkRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final WorkItemLinkRepository workItemLinkRepository;

    public WorkItemDetailResponse getWorkItemDetail(Long workItemId, String lang) {
        // 1. target WorkItem 조회
        WorkItem workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. id=" + workItemId));

        // 2. 해당 작업과 연결된 WorkItemLink 목록 조회
        List<WorkItemLink> links = workItemLinkRepository.findByFromWorkItemIdOrToWorkItemId(workItemId, workItemId);

        // 3. 연결된 상대방 WorkItem들을 LinkedItemResponse DTO로 변환
        List<WorkItemDetailResponse.LinkedItemResponse> linkedItems = links.stream()
                .map(link -> {
                    // 나(workItemId)와 연결된 반대편 작업(target) 식별
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

        // 4. 최종 WorkItemDetailResponse DTO 반환
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
}