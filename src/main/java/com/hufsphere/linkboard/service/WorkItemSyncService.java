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

    private static final String DONE_STATUS = "done";
    private static final String TODO_STATUS = "todo";
    private static final String IN_PROGRESS_STATUS = "in_progress";

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
            List<WorkItemLinkGroupDto> extractedLinks, String lang, SourceType justSyncedSourceType,
            Map<String, Boolean> figmaDoneByUrl, Map<String, Integer> notionCompletionByUrl) {
        Long workspaceId = workspace.getId();

        Map<SourceType, SourceConnection> connectionsByType = sourceConnectionRepository.findByWorkspaceId(workspaceId).stream()
                .collect(Collectors.toMap(SourceConnection::getSourceType, Function.identity(), (existing, duplicate) -> existing));

        // 이번에 실제로 재크롤링하지 않은 소스(justSyncedSourceType 외)는 AI가 오래된 내용을 다시 보고
        // 새로 추측하면서 status가 근거 없이 흔들릴 수 있다(라이브 검증으로 실제 발견됨: Notion만 동기화했는데
        // Figma의 ✅ 기반 done이 todo로 되돌아감). 삭제 전에 URL별 기존 상태를 스냅샷으로 남겨서,
        // 재크롤링 안 한 소스는 이 값을 그대로 이어간다.
        Map<String, WorkItem> previousByUrl = workItemRepository.findByWorkspaceIdOrderBySourceUpdatedAtDesc(workspaceId).stream()
                .collect(Collectors.toMap(WorkItem::getSourceUrl, Function.identity(), (existing, duplicate) -> existing));

        workItemLinkRepository.deleteByWorkspaceId(workspaceId);
        workItemRepository.deleteByWorkspaceId(workspaceId);
        featureRepository.deleteByWorkspaceId(workspaceId);

        Map<Integer, WorkItem> indexToItem = saveWorkItems(workspace, connectionsByType, extractedItems,
                justSyncedSourceType, previousByUrl, figmaDoneByUrl, notionCompletionByUrl);
        saveWorkItemLinks(extractedLinks, indexToItem);

        GroupFeaturesResponse grouped = aiServerClient.groupFeatures(lang);
        saveFeatures(workspace, grouped.getFeatures(), indexToItem);
    }

    private Map<Integer, WorkItem> saveWorkItems(Workspace workspace, Map<SourceType, SourceConnection> connectionsByType,
            List<WorkItemDto> extractedItems, SourceType justSyncedSourceType, Map<String, WorkItem> previousByUrl,
            Map<String, Boolean> figmaDoneByUrl, Map<String, Integer> notionCompletionByUrl) {
        Map<Integer, WorkItem> indexToItem = new HashMap<>();

        for (int i = 0; i < extractedItems.size(); i++) {
            WorkItemDto dto = extractedItems.get(i);
            SourceType sourceType = SourceType.fromValue(dto.getSourceType());
            SourceConnection sourceConnection = connectionsByType.get(sourceType);
            if (sourceConnection == null) {
                // 이 워크스페이스에 더 이상 연결되어 있지 않은 소스 타입의 잔여 색인 데이터 -> 건너뜀
                continue;
            }

            String status = dto.getStatus();
            Integer completionRate = null;

            // Figma는 AI의 텍스트 기반 추측 대신, 코멘트에 달린 ✅ 리액션 유무로 완료 여부를 실측해서
            // status를 덮어쓴다. Notion은 페이지 안 to_do 체크 비율(0~100)로 완료율을 실측해서 status까지
            // 같이 덮어쓴다. 둘 다 "이번에 그 소스를 직접 크롤링했을 때"만 값이 채워져 있다.
            if (sourceType == SourceType.FIGMA && figmaDoneByUrl.containsKey(dto.getUrl())) {
                status = Boolean.TRUE.equals(figmaDoneByUrl.get(dto.getUrl())) ? DONE_STATUS : TODO_STATUS;
            } else if (sourceType == SourceType.NOTION && notionCompletionByUrl.containsKey(dto.getUrl())) {
                completionRate = notionCompletionByUrl.get(dto.getUrl());
                status = completionRate == 0 ? TODO_STATUS : completionRate == 100 ? DONE_STATUS : IN_PROGRESS_STATUS;
            } else if (sourceType != justSyncedSourceType) {
                // 이번에 재크롤링하지 않은 소스: 내용이 안 바뀌었는데 AI가 다시 추측하면서 흔들릴 수 있으니,
                // 기존 값이 있으면(이전에 계산된 적 있으면) 그대로 이어간다. 처음 보는 항목이면 폴백으로
                // AI 추측(dto.getStatus())을 그대로 쓴다.
                WorkItem previous = previousByUrl.get(dto.getUrl());
                if (previous != null) {
                    status = previous.getStatus();
                    completionRate = previous.getCompletionRate();
                }
            }

            WorkItem workItem = WorkItem.builder()
                    .workspace(workspace)
                    .sourceConnection(sourceConnection)
                    .sourceType(sourceType)
                    .itemType(dto.getItemType())
                    .sourceNumber(dto.getSourceNumber())
                    .title(dto.getTitle())
                    .status(status)
                    .completionRate(completionRate)
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
