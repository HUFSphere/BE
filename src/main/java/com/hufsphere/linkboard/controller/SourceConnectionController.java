package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.dto.SourceConnectionResponse;
import com.hufsphere.linkboard.service.SourceConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "SourceConnection", description = "데이터 출처 연동 API")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/source-connections")
@RequiredArgsConstructor
public class SourceConnectionController {

    private final SourceConnectionService sourceConnectionService;

    @Operation(summary = "출처 연동 목록 조회 (6.1)", description = "워크스페이스에 연동된 데이터 출처(GitHub, Jira 등) 목록을 조회합니다.")
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
}