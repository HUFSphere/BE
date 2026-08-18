package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.FigmaConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "Figma 연결 응답")
public class FigmaOAuthConnectionResponse {

    @Schema(description = "워크스페이스 ID", example = "1")
    private final Long workspaceId;

    @Schema(description = "Figma 사용자 핸들", example = "hufsphere")
    private final String figmaUserHandle;

    public static FigmaOAuthConnectionResponse from(FigmaConnection figmaConnection) {
        return new FigmaOAuthConnectionResponse(
                figmaConnection.getWorkspaceId(),
                figmaConnection.getFigmaUserHandle()
        );
    }
}
