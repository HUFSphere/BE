package com.hufsphere.linkboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hufsphere.linkboard.client.GithubOAuthClient;
import com.hufsphere.linkboard.client.dto.GithubRepoItem;
import com.hufsphere.linkboard.domain.GithubConnection;
import com.hufsphere.linkboard.dto.response.GithubRepoResponse;
import com.hufsphere.linkboard.exception.GithubNotConnectedException;
import com.hufsphere.linkboard.repository.GithubConnectionRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GithubOAuthServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private GithubConnectionRepository githubConnectionRepository;
    @Mock
    private GithubOAuthClient githubOAuthClient;

    @InjectMocks
    private GithubOAuthService githubOAuthService;

    private static GithubRepoItem repoItemOf(String fullName, boolean privateRepo, String defaultBranch) {
        GithubRepoItem item = new GithubRepoItem();
        ReflectionTestUtils.setField(item, "fullName", fullName);
        ReflectionTestUtils.setField(item, "privateRepo", privateRepo);
        ReflectionTestUtils.setField(item, "defaultBranch", defaultBranch);
        return item;
    }

    @Test
    void 연결된_GitHub_계정의_레포지토리_목록을_반환한다() {
        GithubConnection connection = GithubConnection.builder()
                .workspaceId(1L)
                .accessToken("token123")
                .githubLogin("jaeyoung123")
                .build();
        when(githubConnectionRepository.findFirstByWorkspaceIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(connection));
        when(githubOAuthClient.listRepos("token123"))
                .thenReturn(List.of(
                        repoItemOf("HUFSphere/BE", false, "main"),
                        repoItemOf("HUFSphere/FE", true, "develop")
                ));

        List<GithubRepoResponse> repos = githubOAuthService.listRepos(1L);

        assertThat(repos).hasSize(2);
        assertThat(repos.get(0).getFullName()).isEqualTo("HUFSphere/BE");
        assertThat(repos.get(0).isPrivateRepo()).isFalse();
        assertThat(repos.get(0).getDefaultBranch()).isEqualTo("main");
        assertThat(repos.get(1).getFullName()).isEqualTo("HUFSphere/FE");
        assertThat(repos.get(1).isPrivateRepo()).isTrue();
    }

    @Test
    void GitHub가_연결되어_있지_않으면_레포지토리_목록_조회시_예외를_던진다() {
        when(githubConnectionRepository.findFirstByWorkspaceIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> githubOAuthService.listRepos(1L))
                .isInstanceOf(GithubNotConnectedException.class);
    }
}
