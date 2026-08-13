package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.dto.response.SourceSyncResponse;
import com.hufsphere.linkboard.service.SourceSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "SourceSync", description = "데이터 출처 수동 동기화 API")
@RestController
@RequestMapping("/api/v1/sources")
@RequiredArgsConstructor
public class SourceSyncController {

    private final SourceSyncService sourceSyncService;

    @Operation(summary = "수동 동기화 요청", description = "특정 데이터 소스의 수동 동기화를 실행합니다.")
    @PostMapping("/{sourceId}/sync")
    public ResponseEntity<ApiResponse<SourceSyncResponse>> sync(
            @Parameter(description = "동기화할 소스 연결 ID", example = "1")
            @PathVariable Long sourceId
    ) {
        SourceSyncResponse response = sourceSyncService.sync(sourceId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("SYNC_STARTED", "동기화를 시작했습니다", response));
    }
}