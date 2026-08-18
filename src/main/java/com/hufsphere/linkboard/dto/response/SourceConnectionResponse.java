package com.hufsphere.linkboard.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hufsphere.linkboard.domain.SourceConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SourceConnectionResponse {

    @Schema(description = "연동 ID", example = "1")
    private final Long id;

    @Schema(description = "소스 타입 (GITHUB, NOTION, FIGMA)", example = "GITHUB")
    private final String sourceType;

    @Schema(description = "대상 레포지토리 또는 보드", example = "HUFSphere/BE")
    private final String targetRepoOrBoard;

    @Schema(description = "연동 상태", example = "CONNECTED")
    private final String status;

    @Schema(description = "마지막 동기화 시각", example = "2026-08-09T15:12:40")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime lastSyncedAt;

    public static SourceConnectionResponse of(SourceConnection sourceConnection) {
        return new SourceConnectionResponse(
                sourceConnection.getId(),
                sourceConnection.getSourceType() != null ? sourceConnection.getSourceType().getValue() : null,
                sourceConnection.getTargetRepoOrBoard(),
                sourceConnection.getStatus(),
                sourceConnection.getLastSyncedAt()
        );
    }
}