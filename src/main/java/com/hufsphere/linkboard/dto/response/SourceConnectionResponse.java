package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.SourceConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "소스 연결 응답")
public class SourceConnectionResponse {

    @Schema(description = "생성된 소스 연결 ID", example = "1")
    private final Long sourceId;

    @Schema(description = "워크스페이스 ID", example = "1")
    private final Long workspaceId;

    @Schema(description = "소스 타입", example = "github")
    private final String sourceType;

    @Schema(description = "소스 식별자", example = "pypa/sampleproject")
    private final String sourceRef;

    @Schema(description = "연결 상태", example = "pending")
    private final String connStatus;

    @Schema(description = "마지막 동기화 시각", example = "null")
    private final LocalDateTime lastSyncedAt;

    @Schema(description = "생성 시각", example = "2026-08-09T15:10:00")
    private final LocalDateTime createdAt;

    public static SourceConnectionResponse from(SourceConnection sourceConnection) {
        return new SourceConnectionResponse(
                sourceConnection.getId(),
                sourceConnection.getWorkspaceId(),
                sourceConnection.getSourceType().getValue(),
                sourceConnection.getSourceRef(),
                sourceConnection.getConnStatus().getValue(),
                sourceConnection.getLastSyncedAt(),
                sourceConnection.getCreatedAt()
        );
    }
}
