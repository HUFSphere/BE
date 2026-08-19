package com.hufsphere.linkboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hufsphere.linkboard.domain.Feature;
import com.hufsphere.linkboard.domain.SourceConnection;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.dto.DashboardFeaturesResponse;
import com.hufsphere.linkboard.dto.DashboardSourcesResponse;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.FeatureRepository;
import com.hufsphere.linkboard.repository.SourceConnectionRepository;
import com.hufsphere.linkboard.repository.WorkItemLinkRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkItemServiceTest {

    @Mock
    private WorkItemRepository workItemRepository;
    @Mock
    private WorkItemLinkRepository workItemLinkRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private FeatureRepository featureRepository;
    @Mock
    private SourceConnectionRepository sourceConnectionRepository;

    @InjectMocks
    private WorkItemService workItemService;

    private static Feature featureOf(long id, String name) {
        Feature feature = Feature.builder().name(name).build();
        ReflectionTestUtils.setField(feature, "id", id);
        return feature;
    }

    private static WorkItem workItemOf(Long featureId, String status, LocalDateTime updatedAt) {
        WorkItem item = WorkItem.builder()
                .status(status)
                .sourceUpdatedAt(updatedAt)
                .build();
        item.setFeatureId(featureId);
        return item;
    }

    @Test
    void 워크스페이스가_없으면_기능별_진행률_조회는_예외를_던진다() {
        when(workspaceRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> workItemService.getFeatureProgress(1L))
                .isInstanceOf(WorkspaceNotFoundException.class);
    }

    @Test
    void 기능별_진행률은_최근_수정순으로_상위_3개만_반환한다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);

        Feature auth = featureOf(101L, "인증/로그인");
        Feature billing = featureOf(102L, "결제");
        Feature search = featureOf(103L, "검색");
        Feature old = featureOf(104L, "레거시");
        when(featureRepository.findByWorkspaceId(1L)).thenReturn(List.of(auth, billing, search, old));

        LocalDateTime t0 = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime t1 = LocalDateTime.of(2026, 8, 2, 0, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 8, 4, 0, 0);
        LocalDateTime t4 = LocalDateTime.of(2026, 8, 5, 0, 0);

        List<WorkItem> items = List.of(
                workItemOf(101L, "done", t3),
                workItemOf(101L, "todo", t0),
                workItemOf(102L, "done", t4),
                workItemOf(103L, "todo", t1),
                workItemOf(104L, "done", t0)
        );
        when(workItemRepository.findByWorkspaceIdOrderBySourceUpdatedAtDesc(1L)).thenReturn(items);

        DashboardFeaturesResponse response = workItemService.getFeatureProgress(1L);

        assertThat(response.getFeatures()).hasSize(3);
        assertThat(response.getFeatures().get(0).getName()).isEqualTo("결제");
        assertThat(response.getFeatures().get(0).getProgress()).isEqualTo(1.0);
        assertThat(response.getFeatures().get(1).getName()).isEqualTo("인증/로그인");
        assertThat(response.getFeatures().get(1).getTotalCount()).isEqualTo(2);
        assertThat(response.getFeatures().get(1).getDoneCount()).isEqualTo(1);
        assertThat(response.getFeatures().get(1).getProgress()).isEqualTo(0.5);
        assertThat(response.getFeatures().get(2).getName()).isEqualTo("검색");
        assertThat(response.getFeatures().get(2).getProgress()).isEqualTo(0.0);
    }

    @Test
    void 소스별_카드는_소스마다_진행률과_최근_이슈_상위_3개를_반환한다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);

        SourceConnection github = SourceConnection.builder()
                .sourceType(SourceType.GITHUB)
                .targetRepoOrBoard("HUFSphere/BE")
                .status("CONNECTED")
                .build();
        github.setId(10L);

        SourceConnection notion = SourceConnection.builder()
                .sourceType(SourceType.NOTION)
                .targetRepoOrBoard("팀 노션")
                .status("CONNECTED")
                .build();
        notion.setId(20L);

        when(sourceConnectionRepository.findByWorkspaceId(1L)).thenReturn(List.of(github, notion));

        LocalDateTime t1 = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 8, 2, 0, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 8, 3, 0, 0);
        LocalDateTime t4 = LocalDateTime.of(2026, 8, 4, 0, 0);

        WorkItem gh1 = WorkItem.builder().sourceConnection(github).status("done").title("PR1").sourceUpdatedAt(t1).build();
        WorkItem gh2 = WorkItem.builder().sourceConnection(github).status("done").title("PR2").sourceUpdatedAt(t2).build();
        WorkItem gh3 = WorkItem.builder().sourceConnection(github).status("todo").title("PR3").sourceUpdatedAt(t3).build();
        WorkItem gh4 = WorkItem.builder().sourceConnection(github).status("todo").title("PR4").sourceUpdatedAt(t4).build();
        WorkItem notionItem = WorkItem.builder().sourceConnection(notion).status("done").title("결정사항").sourceUpdatedAt(t1).build();

        when(workItemRepository.findByWorkspaceIdOrderBySourceUpdatedAtDesc(1L)).thenReturn(List.of(gh1, gh2, gh3, gh4, notionItem));

        DashboardSourcesResponse response = workItemService.getSourceProgress(1L);

        assertThat(response.getSources()).hasSize(2);

        DashboardSourcesResponse.SourceCard githubCard = response.getSources().stream()
                .filter(card -> card.getSourceId().equals(10L))
                .findFirst().orElseThrow();
        assertThat(githubCard.getTotalCount()).isEqualTo(4);
        assertThat(githubCard.getDoneCount()).isEqualTo(2);
        assertThat(githubCard.getProgress()).isEqualTo(0.5);
        assertThat(githubCard.getRecentIssues()).hasSize(3);
        assertThat(githubCard.getRecentIssues().get(0).getTitle()).isEqualTo("PR4");

        DashboardSourcesResponse.SourceCard notionCard = response.getSources().stream()
                .filter(card -> card.getSourceId().equals(20L))
                .findFirst().orElseThrow();
        assertThat(notionCard.getTotalCount()).isEqualTo(1);
        assertThat(notionCard.getProgress()).isEqualTo(1.0);
    }
}
