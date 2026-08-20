package com.hufsphere.linkboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufsphere.linkboard.client.dto.GroupFeaturesResponse;
import com.hufsphere.linkboard.client.dto.GroupedFeatureDto;
import com.hufsphere.linkboard.client.dto.WorkItemDto;
import com.hufsphere.linkboard.domain.Feature;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.repository.FeatureRepository;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import com.hufsphere.linkboard.repository.WorkItemLinkRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkItemSyncServiceTest {

    @Mock
    private WorkItemRepository workItemRepository;
    @Mock
    private WorkItemLinkRepository workItemLinkRepository;
    @Mock
    private SourceConnectionRepository sourceConnectionRepository;
    @Mock
    private FeatureRepository featureRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WorkItemSyncService workItemSyncService;

    private Workspace workspace;
    private SourceConnection githubConnection;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder().build();
        workspace.setId(1L);

        githubConnection = SourceConnection.builder()
                .workspace(workspace)
                .sourceType(SourceType.GITHUB)
                .build();
        githubConnection.setId(10L);
    }

    @Test
    void 동기화하면_기존_기능을_지우고_AI가_분류한_기능을_저장하고_workItem에_연결한다() {
        when(sourceConnectionRepository.findByWorkspaceId(1L)).thenReturn(List.of(githubConnection));

        AtomicLong workItemIdSeq = new AtomicLong(1);
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                ReflectionTestUtils.setField(item, "id", workItemIdSeq.getAndIncrement());
            }
            return item;
        });

        AtomicLong featureIdSeq = new AtomicLong(100);
        when(featureRepository.save(any(Feature.class))).thenAnswer(invocation -> {
            Feature feature = invocation.getArgument(0);
            ReflectionTestUtils.setField(feature, "id", featureIdSeq.getAndIncrement());
            return feature;
        });

        WorkItemDto item0 = new WorkItemDto();
        item0.setSourceType("github");
        item0.setItemType("pr");
        item0.setTitle("Add JWT auth");
        item0.setStatus("done");

        WorkItemDto item1 = new WorkItemDto();
        item1.setSourceType("github");
        item1.setItemType("issue");
        item1.setTitle("Fix login bug");
        item1.setStatus("todo");

        GroupedFeatureDto groupedFeature = new GroupedFeatureDto();
        groupedFeature.setFeatureName("인증/로그인");
        groupedFeature.setFeatureDescription("로그인 관련 작업");
        groupedFeature.setWorkItemIndexes(List.of(0, 1));

        GroupFeaturesResponse groupFeaturesResponse = new GroupFeaturesResponse();
        groupFeaturesResponse.setFeatures(List.of(groupedFeature));

        workItemSyncService.replaceForWorkspace(workspace, List.of(item0, item1), List.of(), groupFeaturesResponse, SourceType.GITHUB, Map.of(), Map.of());

        verify(workItemLinkRepository).deleteByWorkspaceId(1L);
        verify(workItemRepository).deleteByWorkspaceId(1L);
        verify(featureRepository).deleteByWorkspaceId(1L);

        ArgumentCaptor<Feature> savedFeature = ArgumentCaptor.forClass(Feature.class);
        verify(featureRepository).save(savedFeature.capture());
        assertThat(savedFeature.getValue().getName()).isEqualTo("인증/로그인");
        assertThat(savedFeature.getValue().getWorkspace()).isEqualTo(workspace);

        ArgumentCaptor<WorkItem> savedItems = ArgumentCaptor.forClass(WorkItem.class);
        verify(workItemRepository, atLeast(2)).save(savedItems.capture());

        List<WorkItem> distinctSavedItems = savedItems.getAllValues().stream().distinct().toList();
        assertThat(distinctSavedItems).hasSize(2);
        assertThat(distinctSavedItems).allSatisfy(item -> assertThat(item.getFeatureId()).isEqualTo(100L));
    }

    @Test
    void AI가_반환한_기능이_없으면_workItem만_저장하고_feature는_저장하지_않는다() {
        when(sourceConnectionRepository.findByWorkspaceId(1L)).thenReturn(List.of(githubConnection));

        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 1L);
            return item;
        });

        WorkItemDto item0 = new WorkItemDto();
        item0.setSourceType("github");
        item0.setItemType("pr");
        item0.setTitle("Add JWT auth");
        item0.setStatus("done");

        GroupFeaturesResponse emptyResponse = new GroupFeaturesResponse();
        emptyResponse.setFeatures(List.of());

        workItemSyncService.replaceForWorkspace(workspace, List.of(item0), List.of(), emptyResponse, SourceType.GITHUB, Map.of(), Map.of());

        verify(featureRepository).deleteByWorkspaceId(1L);
        verify(featureRepository, org.mockito.Mockito.never()).save(any(Feature.class));
    }

    @Test
    void notion_체크비율이_있으면_completionRate와_status를_실측값으로_덮어쓴다() {
        SourceConnection notionConnection = SourceConnection.builder()
                .workspace(workspace)
                .sourceType(SourceType.NOTION)
                .build();
        notionConnection.setId(20L);
        when(sourceConnectionRepository.findByWorkspaceId(1L)).thenReturn(List.of(notionConnection));

        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupFeaturesResponse emptyResponse = new GroupFeaturesResponse();
        emptyResponse.setFeatures(List.of());

        WorkItemDto partiallyChecked = new WorkItemDto();
        partiallyChecked.setSourceType("notion");
        partiallyChecked.setItemType("meeting");
        partiallyChecked.setTitle("5차 회의");
        partiallyChecked.setStatus("done"); // AI 추측값, 덮어써져야 함
        partiallyChecked.setUrl("https://notion.so/5");

        WorkItemDto noneChecked = new WorkItemDto();
        noneChecked.setSourceType("notion");
        noneChecked.setItemType("meeting");
        noneChecked.setTitle("6차 회의");
        noneChecked.setStatus("in_progress");
        noneChecked.setUrl("https://notion.so/6");

        WorkItemDto fullyChecked = new WorkItemDto();
        fullyChecked.setSourceType("notion");
        fullyChecked.setItemType("meeting");
        fullyChecked.setTitle("7차 회의");
        fullyChecked.setStatus("todo"); // AI 추측값, 덮어써져야 함
        fullyChecked.setUrl("https://notion.so/7");

        WorkItemDto noSignal = new WorkItemDto();
        noSignal.setSourceType("notion");
        noSignal.setItemType("meeting");
        noSignal.setTitle("체크리스트 없는 페이지");
        noSignal.setStatus("blocked"); // 신호 없으니 AI 추측값 그대로 유지되어야 함
        noSignal.setUrl("https://notion.so/no-checklist");

        Map<String, Integer> notionCompletionByUrl = Map.of(
                "https://notion.so/5", 60,
                "https://notion.so/6", 0,
                "https://notion.so/7", 100
        );

        workItemSyncService.replaceForWorkspace(
                workspace, List.of(partiallyChecked, noneChecked, fullyChecked, noSignal), List.of(), emptyResponse,
                SourceType.NOTION, Map.of(), notionCompletionByUrl);

        ArgumentCaptor<WorkItem> savedItems = ArgumentCaptor.forClass(WorkItem.class);
        verify(workItemRepository, atLeast(4)).save(savedItems.capture());
        List<WorkItem> distinctSavedItems = savedItems.getAllValues().stream().distinct().toList();

        WorkItem partial = distinctSavedItems.stream().filter(i -> "5차 회의".equals(i.getTitle())).findFirst().orElseThrow();
        assertThat(partial.getCompletionRate()).isEqualTo(60);
        assertThat(partial.getStatus()).isEqualTo("in_progress");

        WorkItem none = distinctSavedItems.stream().filter(i -> "6차 회의".equals(i.getTitle())).findFirst().orElseThrow();
        assertThat(none.getCompletionRate()).isEqualTo(0);
        assertThat(none.getStatus()).isEqualTo("todo");

        WorkItem full = distinctSavedItems.stream().filter(i -> "7차 회의".equals(i.getTitle())).findFirst().orElseThrow();
        assertThat(full.getCompletionRate()).isEqualTo(100);
        assertThat(full.getStatus()).isEqualTo("done");

        WorkItem noChecklist = distinctSavedItems.stream().filter(i -> "체크리스트 없는 페이지".equals(i.getTitle())).findFirst().orElseThrow();
        assertThat(noChecklist.getCompletionRate()).isNull();
        assertThat(noChecklist.getStatus()).isEqualTo("blocked");
    }

    @Test
    void 이번에_재크롤링하지_않은_소스는_AI_재추측_대신_기존_status를_그대로_이어간다() {
        SourceConnection figmaConnection = SourceConnection.builder()
                .workspace(workspace)
                .sourceType(SourceType.FIGMA)
                .build();
        figmaConnection.setId(30L);
        when(sourceConnectionRepository.findByWorkspaceId(1L)).thenReturn(List.of(githubConnection, figmaConnection));

        // Figma는 예전에 ✅ 체크마크로 done이 확정되어 저장돼 있던 상태(스냅샷)
        WorkItem previousFigmaItem = WorkItem.builder()
                .workspace(workspace)
                .sourceConnection(figmaConnection)
                .sourceType(SourceType.FIGMA)
                .itemType("design")
                .title("대시보드 페이지")
                .status("done")
                .sourceUrl("https://figma.com/dashboard")
                .build();
        when(workItemRepository.findByWorkspaceIdOrderBySourceUpdatedAtDesc(1L)).thenReturn(List.of(previousFigmaItem));

        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupFeaturesResponse emptyResponse = new GroupFeaturesResponse();
        emptyResponse.setFeatures(List.of());

        // 이번엔 GitHub만 동기화하는 중 -> Figma는 재크롤링 안 됐으니 figmaDoneByUrl이 비어있고,
        // AI는 (오래된 내용을 다시 보고) todo로 잘못 재추측함
        WorkItemDto staleFigmaGuess = new WorkItemDto();
        staleFigmaGuess.setSourceType("figma");
        staleFigmaGuess.setItemType("design");
        staleFigmaGuess.setTitle("대시보드 페이지");
        staleFigmaGuess.setStatus("todo");
        staleFigmaGuess.setUrl("https://figma.com/dashboard");

        WorkItemDto newFigmaItem = new WorkItemDto();
        newFigmaItem.setSourceType("figma");
        newFigmaItem.setItemType("design");
        newFigmaItem.setTitle("처음 보는 프레임");
        newFigmaItem.setStatus("todo");
        newFigmaItem.setUrl("https://figma.com/new-frame");

        workItemSyncService.replaceForWorkspace(
                workspace, List.of(staleFigmaGuess, newFigmaItem), List.of(), emptyResponse, SourceType.GITHUB, Map.of(), Map.of());

        ArgumentCaptor<WorkItem> savedItems = ArgumentCaptor.forClass(WorkItem.class);
        verify(workItemRepository, atLeast(2)).save(savedItems.capture());
        List<WorkItem> distinctSavedItems = savedItems.getAllValues().stream().distinct().toList();

        WorkItem carriedOver = distinctSavedItems.stream().filter(i -> "대시보드 페이지".equals(i.getTitle())).findFirst().orElseThrow();
        assertThat(carriedOver.getStatus()).isEqualTo("done"); // AI의 todo 재추측이 아니라 기존 값 유지

        WorkItem brandNew = distinctSavedItems.stream().filter(i -> "처음 보는 프레임".equals(i.getTitle())).findFirst().orElseThrow();
        assertThat(brandNew.getStatus()).isEqualTo("todo"); // 기존 기록이 없으니 AI 추측을 그대로 씀

        verify(notificationService).notifyNewWorkItemsDetected(workspace, 1); // "처음 보는 프레임" 1건만 신규
    }

    @Test
    void 워크스페이스의_첫_동기화면_모든_항목이_새로_잡히므로_새_작업_항목_알림을_보내지_않는다() {
        when(sourceConnectionRepository.findByWorkspaceId(1L)).thenReturn(List.of(githubConnection));
        when(workItemRepository.findByWorkspaceIdOrderBySourceUpdatedAtDesc(1L)).thenReturn(List.of());
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupFeaturesResponse emptyResponse = new GroupFeaturesResponse();
        emptyResponse.setFeatures(List.of());

        WorkItemDto item0 = new WorkItemDto();
        item0.setSourceType("github");
        item0.setItemType("pr");
        item0.setTitle("Add JWT auth");
        item0.setStatus("done");
        item0.setUrl("https://github.com/pr/1");

        workItemSyncService.replaceForWorkspace(workspace, List.of(item0), List.of(), emptyResponse, SourceType.GITHUB, Map.of(), Map.of());

        verify(notificationService, org.mockito.Mockito.never()).notifyNewWorkItemsDetected(any(), anyInt());
    }
}
