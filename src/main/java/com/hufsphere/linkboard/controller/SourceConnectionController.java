package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.dto.request.SourceConnectionCreateRequest;
import com.hufsphere.linkboard.dto.response.SourceConnectionResponse;
import com.hufsphere.linkboard.dto.response.SourceSyncResponse;
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

    @Operation(summary = "연동 생성", description = "새로운 데이터 소스 연동을 추가합니다.")
    @PostMapping
    public ResponseEntity<?> createSourceConnection(
            @PathVariable Long workspaceId,
            @RequestBody SourceConnectionCreateRequest request
    ) {
        SourceConnectionResponse response = sourceConnectionService.createConnection(workspaceId, request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "SOURCE_CONNECTION_CREATED",
                "message", "데이터 출처 연동 성공",
                "data", response
        ));
    }

    @Operation(summary = "연동 목록 조회", description = "워크스페이스의 데이터 출처 연동 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getSourceConnections(@PathVariable Long workspaceId) {
        List<SourceConnectionResponse> responses = sourceConnectionService.getConnections(workspaceId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "SOURCE_CONNECTION_LIST_OK",
                "message", "데이터 출처 연동 목록 조회 성공",
                "data", responses
        ));
    }
}