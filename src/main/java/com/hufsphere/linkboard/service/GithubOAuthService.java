package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.GithubOAuthClient;
import com.hufsphere.linkboard.client.dto.GithubTokenExchangeResponse;
import com.hufsphere.linkboard.client.dto.GithubUserResponse;
import com.hufsphere.linkboard.domain.GithubConnection;
import com.hufsphere.linkboard.dto.response.GithubOAuthConnectionResponse;
import com.hufsphere.linkboard.dto.response.GithubRepoResponse;
import com.hufsphere.linkboard.exception.GithubNotConnectedException;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.GithubConnectionRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GithubOAuthService {

    private final WorkspaceRepository workspaceRepository;
    private final GithubConnectionRepository githubConnectionRepository;
    private final GithubOAuthClient githubOAuthClient;

    @Transactional
    public GithubOAuthConnectionResponse connect(Long workspaceId, String code) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다");
        }

        GithubTokenExchangeResponse tokenResponse = githubOAuthClient.exchangeToken(code);
        GithubUserResponse userResponse = githubOAuthClient.fetchUser(tokenResponse.getAccessToken());

        GithubConnection githubConnection = GithubConnection.builder()
                .workspaceId(workspaceId)
                .accessToken(tokenResponse.getAccessToken())
                .githubUserId(userResponse.getId() != null ? String.valueOf(userResponse.getId()) : null)
                .githubLogin(userResponse.getLogin())
                .build();

        GithubConnection saved = githubConnectionRepository.save(githubConnection);
        return GithubOAuthConnectionResponse.from(saved);
    }

    public List<GithubRepoResponse> listRepos(Long workspaceId) {
        GithubConnection githubConnection = githubConnectionRepository
                .findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> new GithubNotConnectedException("먼저 GitHub를 연결해주세요"));

        return githubOAuthClient.listRepos(githubConnection.getAccessToken()).stream()
                .map(GithubRepoResponse::from)
                .toList();
    }
}
