package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.GithubConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "GitHub 연결 응답")
public class GithubOAuthConnectionResponse {

    @Schema(description = "워크스페이스 ID", example = "1")
    private final Long workspaceId;

    @Schema(description = "GitHub 로그인 아이디", example = "jaeyoung123")
    private final String githubLogin;

    public static GithubOAuthConnectionResponse from(GithubConnection githubConnection) {
        return new GithubOAuthConnectionResponse(
                githubConnection.getWorkspaceId(),
                githubConnection.getGithubLogin()
        );
    }
}
