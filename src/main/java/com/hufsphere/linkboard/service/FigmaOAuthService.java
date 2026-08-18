package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.FigmaOAuthClient;
import com.hufsphere.linkboard.client.dto.FigmaTokenExchangeResponse;
import com.hufsphere.linkboard.client.dto.FigmaUserResponse;
import com.hufsphere.linkboard.domain.FigmaConnection;
import com.hufsphere.linkboard.dto.response.FigmaOAuthConnectionResponse;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.FigmaConnectionRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FigmaOAuthService {

    private final WorkspaceRepository workspaceRepository;
    private final FigmaConnectionRepository figmaConnectionRepository;
    private final FigmaOAuthClient figmaOAuthClient;

    @Transactional
    public FigmaOAuthConnectionResponse connect(Long workspaceId, String code) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다");
        }

        FigmaTokenExchangeResponse tokenResponse = figmaOAuthClient.exchangeToken(code);
        FigmaUserResponse userResponse = figmaOAuthClient.fetchUser(tokenResponse.getAccessToken());

        FigmaConnection figmaConnection = FigmaConnection.builder()
                .workspaceId(workspaceId)
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .figmaUserId(userResponse.getId())
                .figmaUserHandle(userResponse.getHandle())
                .build();

        FigmaConnection saved = figmaConnectionRepository.save(figmaConnection);
        return FigmaOAuthConnectionResponse.from(saved);
    }
}
