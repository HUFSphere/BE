package com.hufsphere.linkboard.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hufsphere.linkboard.domain.SourceConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SourceSyncResponse {

    @Schema(description = "연동 ID", example = "1")
    private final Long id;

    @Schema(description = "연동 상태", example = "SYNCING")
    private final String status;

    @Schema(description = "동기화 시작 시각", example = "2026-08-09T15:12:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime startedAt;

    public static SourceSyncResponse of(SourceConnection sourceConnection, LocalDateTime startedAt) {
        return new SourceSyncResponse(
                sourceConnection.getId(),
                sourceConnection.getStatus(),
                startedAt
        );
    }
}