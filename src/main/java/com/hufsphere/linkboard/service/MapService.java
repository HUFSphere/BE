package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.WorkItemLink;
import com.hufsphere.linkboard.dto.MapResponse;
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
public class MapService {

    private final WorkItemRepository workItemRepository;
    private final WorkItemLinkRepository workItemLinkRepository;

    public MapResponse getProjectMap(Long workspaceId, String lang, String sourceType) {
        // 1. sourceType 필터링 조건에 따른 WorkItem(노드) 조회
        List<WorkItem> workItems;
        if (sourceType != null && !sourceType.isBlank()) {
            SourceType type = SourceType.valueOf(sourceType.toLowerCase());
            workItems = workItemRepository.findByWorkspaceIdAndSourceType(workspaceId, type);
        } else {
            workItems = workItemRepository.findByWorkspaceId(workspaceId);
        }

        // 2. WorkItem Entity -> MapResponse.NodeResponse DTO 변환
        List<MapResponse.NodeResponse> nodes = workItems.stream()
                .map(item -> MapResponse.NodeResponse.builder()
                        .id(item.getId())
                        .sourceType(item.getSourceType() != null ? item.getSourceType().name().toLowerCase() : null)
                        .itemType(item.getItemType())
                        .sourceNumber(item.getSourceNumber())
                        .title(item.getTitle())
                        .status(item.getStatus())
                        .summaryNative(item.getSummaryNative())
                        .authorLogin(item.getAuthorLogin())
                        .sourceUrl(item.getSourceUrl())
                        .sourceUpdatedAt(item.getSourceUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        // 3. WorkItemLink(연결선) 조회 및 DTO 변환
        List<WorkItemLink> workItemLinks = workItemLinkRepository.findByFromWorkItemWorkspaceId(workspaceId);
        List<MapResponse.LinkResponse> links = workItemLinks.stream()
                .map(link -> MapResponse.LinkResponse.builder()
                        .fromWorkItemId(link.getFromWorkItem().getId())
                        .toWorkItemId(link.getToWorkItem().getId())
                        .linkSource(link.getLinkSource())
                        .build())
                .collect(Collectors.toList());

        // 4. 최종 MapResponse DTO 반환
        return MapResponse.builder()
                .nodes(nodes)
                .links(links)
                .build();
    }
}