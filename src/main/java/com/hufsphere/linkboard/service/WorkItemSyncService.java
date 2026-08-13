package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.dto.LinkedWorkItemDto;
import com.hufsphere.linkboard.client.dto.WorkItemDto;
import com.hufsphere.linkboard.client.dto.WorkItemLinkGroupDto;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.WorkItemLink;
import com.hufsphere.linkboard.repository.WorkItemLinkRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkItemSyncService {

    private final WorkItemRepository workItemRepository;
    private final WorkItemLinkRepository workItemLinkRepository;

    @Transactional
    public void replace(SourceConnection sourceConnection, List<WorkItemDto> extractedItems, List<WorkItemLinkGroupDto> extractedLinks) {
        Long sourceConnectionId = sourceConnection.getId();

        // work_item_link가 work_item을 참조하므로 링크를 먼저 정리한다.
        workItemLinkRepository.deleteBySourceConnectionId(sourceConnectionId);
        workItemRepository.deleteBySourceConnectionId(sourceConnectionId);

        Map<Integer, Long> indexToId = saveWorkItems(sourceConnection, extractedItems);
        saveWorkItemLinks(extractedLinks, indexToId);
    }

    private Map<Integer, Long> saveWorkItems(SourceConnection sourceConnection, List<WorkItemDto> extractedItems) {
        Map<Integer, Long> indexToId = new HashMap<>();

        for (int i = 0; i < extractedItems.size(); i++) {
            WorkItemDto dto = extractedItems.get(i);

            WorkItem workItem = WorkItem.builder()
                    .workspace(sourceConnection.getWorkspace())
                    .sourceConnection(sourceConnection)
                    .sourceType(SourceType.fromValue(dto.getSourceType()))
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
            indexToId.put(i, saved.getId());
        }

        return indexToId;
    }

    private void saveWorkItemLinks(List<WorkItemLinkGroupDto> extractedLinks, Map<Integer, Long> indexToId) {
        for (WorkItemLinkGroupDto group : extractedLinks) {
            Long fromId = indexToId.get(group.getFromIndex());
            if (fromId == null || group.getLinkedItems() == null) {
                continue;
            }

            for (LinkedWorkItemDto linkedItem : group.getLinkedItems()) {
                Long toId = indexToId.get(linkedItem.getToIndex());
                if (toId == null) {
                    continue;
                }

                WorkItemLink link = WorkItemLink.builder()
                        .fromWorkItem(workItemRepository.getReferenceById(fromId))
                        .toWorkItem(workItemRepository.getReferenceById(toId))
                        .linkSource("auto")
                        .linkReason(linkedItem.getLinkReason())
                        .build();

                workItemLinkRepository.save(link);
            }
        }
    }
}
