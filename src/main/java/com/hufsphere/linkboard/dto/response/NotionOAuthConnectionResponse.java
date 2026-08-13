package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.NotionConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "Notion 연결 응답")
public class NotionOAuthConnectionResponse {

    @Schema(description = "워크스페이스 ID", example = "1")
    private final Long workspaceId;

    @Schema(description = "Notion 워크스페이스 이름", example = "HUFSphere")
    private final String notionWorkspaceName;

    public static NotionOAuthConnectionResponse from(NotionConnection notionConnection) {
        return new NotionOAuthConnectionResponse(
                notionConnection.getWorkspaceId(),
                notionConnection.getNotionWorkspaceName()
        );
    }
}
