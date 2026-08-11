package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.SourceConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "소스 동기화 응답")
public class SourceSyncResponse {

    @Schema(description = "동기화한 소스 연결 ID", example = "1")
    private final Long sourceId;

    @Schema(description = "동기화 완료 후 연결 상태", example = "done")
    private final String connStatus;

    @Schema(description = "동기화 시작 시각", example = "2026-08-09T15:12:00")
    private final LocalDateTime startedAt;

    public static SourceSyncResponse of(SourceConnection sourceConnection, LocalDateTime startedAt) {
        return new SourceSyncResponse(
                sourceConnection.getId(),
                sourceConnection.getConnStatus().getValue(),
                startedAt
        );
    }
}
