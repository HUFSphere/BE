package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.NotionOAuthClient;
import com.hufsphere.linkboard.client.dto.NotionTokenExchangeResponse;
import com.hufsphere.linkboard.domain.NotionConnection;
import com.hufsphere.linkboard.dto.response.NotionOAuthConnectionResponse;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.NotionConnectionRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotionOAuthService {

    private final WorkspaceRepository workspaceRepository;
    private final NotionConnectionRepository notionConnectionRepository;
    private final NotionOAuthClient notionOAuthClient;

    @Transactional
    public NotionOAuthConnectionResponse connect(Long workspaceId, String code) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다");
        }

        NotionTokenExchangeResponse tokenResponse = notionOAuthClient.exchangeToken(code);

        NotionConnection notionConnection = NotionConnection.builder()
                .workspaceId(workspaceId)
                .accessToken(tokenResponse.getAccessToken())
                .notionWorkspaceId(tokenResponse.getWorkspaceId())
                .notionWorkspaceName(tokenResponse.getWorkspaceName())
                .botId(tokenResponse.getBotId())
                .build();

        NotionConnection saved = notionConnectionRepository.save(notionConnection);
        return NotionOAuthConnectionResponse.from(saved);
    }
}
