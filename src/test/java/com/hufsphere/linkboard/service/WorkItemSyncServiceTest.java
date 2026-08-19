package com.hufsphere.linkboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufsphere.linkboard.client.AiServerClient;
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
    private AiServerClient aiServerClient;

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
        when(aiServerClient.groupFeatures("ko")).thenReturn(groupFeaturesResponse);

        workItemSyncService.replaceForWorkspace(workspace, List.of(item0, item1), List.of(), "ko");

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
        when(aiServerClient.groupFeatures("ko")).thenReturn(emptyResponse);

        workItemSyncService.replaceForWorkspace(workspace, List.of(item0), List.of(), "ko");

        verify(featureRepository).deleteByWorkspaceId(1L);
        verify(featureRepository, org.mockito.Mockito.never()).save(any(Feature.class));
    }
}
