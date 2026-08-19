package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.dto.GroupFeaturesResponse;
import com.hufsphere.linkboard.client.dto.GroupedFeatureDto;
import com.hufsphere.linkboard.client.dto.LinkedWorkItemDto;
import com.hufsphere.linkboard.client.dto.WorkItemDto;
import com.hufsphere.linkboard.client.dto.WorkItemLinkGroupDto;
import com.hufsphere.linkboard.domain.Feature;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.WorkItemLink;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.repository.FeatureRepository;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import com.hufsphere.linkboard.repository.WorkItemLinkRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkItemSyncService {

    private final WorkItemRepository workItemRepository;
    private final WorkItemLinkRepository workItemLinkRepository;
    private final SourceConnectionRepository sourceConnectionRepository;
    private final FeatureRepository featureRepository;
    private final AiServerClient aiServerClient;

    // AI 서버의 extract/link/group-features는 요청을 보낸 소스 하나가 아니라 워크스페이스에
    // 색인된 전체(github+notion+...)를 대상으로 응답한다 (라이브 검증으로 확인됨).
    // 그래서 소스 단위가 아니라 워크스페이스 단위로 통째로 삭제 후 다시 채워야
    // 서로 다른 소스 간 work_item_link(예: notion 결정 <-> github PR)와 기능 분류가 유지된다.
    @Transactional
    public void replaceForWorkspace(Workspace workspace, List<WorkItemDto> extractedItems,
            List<WorkItemLinkGroupDto> extractedLinks, String lang) {
        Long workspaceId = workspace.getId();

        Map<SourceType, SourceConnection> connectionsByType = sourceConnectionRepository.findByWorkspaceId(workspaceId).stream()
                .collect(Collectors.toMap(SourceConnection::getSourceType, Function.identity(), (existing, duplicate) -> existing));

        workItemLinkRepository.deleteByWorkspaceId(workspaceId);
        workItemRepository.deleteByWorkspaceId(workspaceId);
        featureRepository.deleteByWorkspaceId(workspaceId);

        Map<Integer, WorkItem> indexToItem = saveWorkItems(workspace, connectionsByType, extractedItems);
        saveWorkItemLinks(extractedLinks, indexToItem);

        GroupFeaturesResponse grouped = aiServerClient.groupFeatures(lang);
        saveFeatures(workspace, grouped.getFeatures(), indexToItem);
    }

    private Map<Integer, WorkItem> saveWorkItems(Workspace workspace, Map<SourceType, SourceConnection> connectionsByType,
            List<WorkItemDto> extractedItems) {
        Map<Integer, WorkItem> indexToItem = new HashMap<>();

        for (int i = 0; i < extractedItems.size(); i++) {
            WorkItemDto dto = extractedItems.get(i);
            SourceType sourceType = SourceType.fromValue(dto.getSourceType());
            SourceConnection sourceConnection = connectionsByType.get(sourceType);
            if (sourceConnection == null) {
                // 이 워크스페이스에 더 이상 연결되어 있지 않은 소스 타입의 잔여 색인 데이터 -> 건너뜀
                continue;
            }

            WorkItem workItem = WorkItem.builder()
                    .workspace(workspace)
                    .sourceConnection(sourceConnection)
                    .sourceType(sourceType)
                    .itemType(dto.getItemType())
                    .sourceNumber(dto.getSourceNumber())
                    .title(dto.getTitle())
                    .status(dto.getStatus())
                    .summaryNative(dto.getSummaryBrief())
                    .content(dto.getContent())
                    .authorLogin(dto.getAuthorLogin())
                    .sourceUrl(dto.getUrl())
                    .sourceUpdatedAt(dto.getSourceUpdatedAt())
                    .build();

            WorkItem saved = workItemRepository.save(workItem);
            indexToItem.put(i, saved);
        }

        return indexToItem;
    }

    private void saveWorkItemLinks(List<WorkItemLinkGroupDto> extractedLinks, Map<Integer, WorkItem> indexToItem) {
        for (WorkItemLinkGroupDto group : extractedLinks) {
            WorkItem fromItem = indexToItem.get(group.getFromIndex());
            if (fromItem == null || group.getLinkedItems() == null) {
                continue;
            }

            for (LinkedWorkItemDto linkedItem : group.getLinkedItems()) {
                WorkItem toItem = indexToItem.get(linkedItem.getToIndex());
                if (toItem == null) {
                    continue;
                }

                WorkItemLink link = WorkItemLink.builder()
                        .fromWorkItem(fromItem)
                        .toWorkItem(toItem)
                        .linkSource("auto")
                        .linkReason(linkedItem.getLinkReason())
                        .build();

                workItemLinkRepository.save(link);
            }
        }
    }

    private void saveFeatures(Workspace workspace, List<GroupedFeatureDto> groupedFeatures, Map<Integer, WorkItem> indexToItem) {
        if (groupedFeatures == null) {
            return;
        }

        for (GroupedFeatureDto groupedFeature : groupedFeatures) {
            Feature feature = Feature.builder()
                    .workspace(workspace)
                    .name(groupedFeature.getFeatureName())
                    .description(groupedFeature.getFeatureDescription())
                    .build();
            Feature saved = featureRepository.save(feature);

            if (groupedFeature.getWorkItemIndexes() == null) {
                continue;
            }

            for (Integer index : groupedFeature.getWorkItemIndexes()) {
                WorkItem workItem = indexToItem.get(index);
                if (workItem == null) {
                    continue;
                }

                workItem.setFeatureId(saved.getId());
                workItemRepository.save(workItem);
            }
        }
    }
}
