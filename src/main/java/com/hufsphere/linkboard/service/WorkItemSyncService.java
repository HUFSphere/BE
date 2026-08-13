package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.dto.LinkedWorkItemDto;
import com.hufsphere.linkboard.client.dto.WorkItemDto;
import com.hufsphere.linkboard.client.dto.WorkItemLinkGroupDto;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.WorkItemLink;
import com.hufsphere.linkboard.domain.Workspace;
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

    // AI 서버의 extract/link-work-items는 요청을 보낸 소스 하나가 아니라 워크스페이스에
    // 색인된 전체(github+notion+...)를 대상으로 응답한다 (라이브 검증으로 확인됨).
    // 그래서 소스 단위가 아니라 워크스페이스 단위로 통째로 삭제 후 다시 채워야
    // 서로 다른 소스 간 work_item_link(예: notion 결정 <-> github PR)가 유지된다.
    @Transactional
    public void replaceForWorkspace(Workspace workspace, List<WorkItemDto> extractedItems, List<WorkItemLinkGroupDto> extractedLinks) {
        Long workspaceId = workspace.getId();

        Map<SourceType, SourceConnection> connectionsByType = sourceConnectionRepository.findByWorkspaceId(workspaceId).stream()
                .collect(Collectors.toMap(SourceConnection::getSourceType, Function.identity(), (existing, duplicate) -> existing));

        workItemLinkRepository.deleteByWorkspaceId(workspaceId);
        workItemRepository.deleteByWorkspaceId(workspaceId);

        Map<Integer, Long> indexToId = saveWorkItems(workspace, connectionsByType, extractedItems);
        saveWorkItemLinks(extractedLinks, indexToId);
    }

    private Map<Integer, Long> saveWorkItems(Workspace workspace, Map<SourceType, SourceConnection> connectionsByType,
            List<WorkItemDto> extractedItems) {
        Map<Integer, Long> indexToId = new HashMap<>();

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
