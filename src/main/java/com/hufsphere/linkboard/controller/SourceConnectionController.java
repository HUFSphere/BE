package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.dto.SourceConnectionResponse;
import com.hufsphere.linkboard.dto.SourceSyncResponse;
import com.hufsphere.linkboard.dto.request.SourceConnectionCreateRequest;
import com.hufsphere.linkboard.service.SourceConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "SourceConnection", description = "데이터 출처 연동 및 동기화 API")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/source-connections")
@RequiredArgsConstructor
public class SourceConnectionController {

    private final SourceConnectionService sourceConnectionService;

    @Operation(summary = "출처 연동 목록 조회 (6.1)", description = "워크스페이스에 연동된 데이터 출처 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getSourceConnections(@PathVariable Long workspaceId) {
        List<SourceConnectionResponse> response = sourceConnectionService.getSourceConnections(workspaceId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "SOURCE_CONN_OK",
                "message", "출처 연동 목록 조회 성공",
                "data", response
        ));
    }

    @Operation(summary = "출처 연동 생성 (6.2)", description = "새로운 데이터 출처를 워크스페이스에 연동합니다.")
    @PostMapping
    public ResponseEntity<?> createSourceConnection(
            @PathVariable Long workspaceId,
            @RequestBody SourceConnectionCreateRequest request
    ) {
        SourceConnectionResponse response = sourceConnectionService.createSourceConnection(workspaceId, request);

        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "code", "SOURCE_CONN_CREATED",
                "message", "출처 연동 생성 성공",
                "data", response
        ));
    }

    @Operation(summary = "출처 수동 동기화 (6.3)", description = "지정한 데이터 출처의 수동 동기화를 요청합니다.")
    @PostMapping("/{connectionId}/sync")
    public ResponseEntity<?> triggerSync(
            @PathVariable Long workspaceId,
            @PathVariable Long connectionId
    ) {
        SourceSyncResponse response = sourceConnectionService.triggerSync(connectionId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "SYNC_STARTED",
                "message", "동기화가 성공적으로 시작되었습니다.",
                "data", response
        ));
    }

    @Operation(summary = "연동 상태 조회 (6.4)", description = "특정 데이터 출처 연동의 상세 상태 및 동기화 이력을 조회합니다.")
    @GetMapping("/{connectionId}/status")
    public ResponseEntity<?> getConnectionStatus(
            @PathVariable Long workspaceId,
            @PathVariable Long connectionId
    ) {
        SourceSyncResponse response = sourceConnectionService.getConnectionStatus(connectionId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "STATUS_OK",
                "message", "연동 상태 조회 성공",
                "data", response
        ));
    }
}