package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.dto.MapResponse;
import com.hufsphere.linkboard.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Map", description = "프로젝트 지도 API")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @Operation(summary = "프로젝트 지도 조회", description = "워크스페이스의 지도 노드 및 링크 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getProjectMap(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String sourceType
    ) {
        MapResponse mapResponse = mapService.getProjectMap(workspaceId, lang, sourceType);

        // 명세서 규격에 맞춘 공통 응답 객체 구성
        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "MAP_OK",
                "message", "프로젝트 지도 조회 성공",
                "data", mapResponse
        ));
    }
}