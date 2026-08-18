package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.client.dto.TeamNormRequestDto;
import com.hufsphere.linkboard.client.dto.TeamNormResponseDto;
import com.hufsphere.linkboard.service.TeamNormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "TeamNorms", description = "팀 관행(Team Norms) 관리 API (6번)")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/norms")
@RequiredArgsConstructor
public class TeamNormController {

    private final TeamNormService teamNormService;

    @Operation(summary = "팀 관행 목록 조회 (6.1)", description = "워크스페이스의 팀 관행 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getNorms(@PathVariable Long workspaceId) {
        List<TeamNormResponseDto> response = teamNormService.getNorms(workspaceId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "TEAM_NORMS_OK",
                "message", "팀 관행 목록 조회 성공",
                "data", response
        ));
    }

    @Operation(summary = "팀 관행 등록 (6.2)", description = "새로운 팀 관행을 등록합니다.")
    @PostMapping
    public ResponseEntity<?> createNorm(
            @PathVariable Long workspaceId,
            @RequestBody TeamNormRequestDto request
    ) {
        TeamNormResponseDto response = teamNormService.createNorm(workspaceId, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "TEAM_NORM_CREATED",
                "message", "팀 관행 등록 성공",
                "data", response
        ));
    }

    @Operation(summary = "팀 관행 수정 (6.3)", description = "등록된 팀 관행을 수정합니다.")
    @PutMapping("/{normId}")
    public ResponseEntity<?> updateNorm(
            @PathVariable Long workspaceId,
            @PathVariable Long normId,
            @RequestBody TeamNormRequestDto request
    ) {
        TeamNormResponseDto response = teamNormService.updateNorm(workspaceId, normId, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "TEAM_NORM_UPDATED",
                "message", "팀 관행 수정 성공",
                "data", response
        ));
    }

    @Operation(summary = "팀 관행 삭제 (6.4)", description = "팀 관행을 삭제합니다.")
    @DeleteMapping("/{normId}")
    public ResponseEntity<?> deleteNorm(
            @PathVariable Long workspaceId,
            @PathVariable Long normId
    ) {
        teamNormService.deleteNorm(workspaceId, normId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "TEAM_NORM_DELETED",
                "message", "팀 관행 삭제 성공"
        ));
    }
}