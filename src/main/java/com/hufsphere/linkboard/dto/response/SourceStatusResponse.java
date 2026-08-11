package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.SourceConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "소스 상태 조회 응답")
public class SourceStatusResponse {

    @Schema(description = "소스 연결 ID", example = "1")
    private final Long sourceId;

    @Schema(description = "소스 타입", example = "github")
    private final String sourceType;

    @Schema(description = "소스 식별자", example = "pypa/sampleproject")
    private final String sourceRef;

    @Schema(description = "연결 상태", example = "indexing")
    private final String connStatus;

    @Schema(description = "인덱싱된 아이템 개수", example = "6")
    private final int indexedCount;

    @Schema(description = "마지막 동기화 시각", example = "2026-08-09T15:12:40")
    private final LocalDateTime lastSyncedAt;

    public static SourceStatusResponse of(SourceConnection sourceConnection, int indexedCount) {
        return new SourceStatusResponse(
                sourceConnection.getId(),
                sourceConnection.getSourceType().getValue(),
                sourceConnection.getSourceRef(),
                sourceConnection.getConnStatus().getValue(),
                indexedCount,
                sourceConnection.getLastSyncedAt()
        );
    }
}
