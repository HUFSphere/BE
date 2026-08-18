package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.client.dto.TeamNormRequestDto;
import com.hufsphere.linkboard.client.dto.TeamNormResponseDto;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.service.TeamNormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "팀 관행 목록 조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "TEAM_NORMS_OK",
                              "message": "팀 관행 목록 조회 성공",
                              "data": [
                                {
                                  "id": 1,
                                  "workspaceId": 1,
                                  "category": "COMMUNICATION",
                                  "content": "오후 6시 이후의 급한 연락은 슬랙 멘션으로 남겨주세요.",
                                  "createdAt": "2026-08-14T18:00:00",
                                  "updatedAt": "2026-08-14T18:00:00"
                                }
                              ]
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다. id=1",
                                      "path": "/api/v1/workspaces/1/norms"
                                    }"""))),
    })
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "팀 관행 등록 성공",
                    content = @Content(schema = @Schema(implementation = TeamNormResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "TEAM_NORM_CREATED",
                                      "message": "팀 관행 등록 성공",
                                      "data": {
                                        "id": 1,
                                        "workspaceId": 1,
                                        "category": "COMMUNICATION",
                                        "content": "오후 6시 이후의 급한 연락은 슬랙 멘션으로 남겨주세요.",
                                        "createdAt": "2026-08-14T18:00:00",
                                        "updatedAt": "2026-08-14T18:00:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "워크스페이스를 찾을 수 없습니다. id=1",
                                      "path": "/api/v1/workspaces/1/norms"
                                    }"""))),
    })
    @PostMapping
    public ResponseEntity<?> createNorm(
            @PathVariable Long workspaceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = TeamNormRequestDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "category": "COMMUNICATION",
                                      "content": "오후 6시 이후의 급한 연락은 슬랙 멘션으로 남겨주세요."
                                    }"""))
            )
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "팀 관행 수정 성공",
                    content = @Content(schema = @Schema(implementation = TeamNormResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "TEAM_NORM_UPDATED",
                                      "message": "팀 관행 수정 성공",
                                      "data": {
                                        "id": 1,
                                        "workspaceId": 1,
                                        "category": "COMMUNICATION",
                                        "content": "오후 6시 이후의 급한 연락은 슬랙 멘션 대신 이메일로 남겨주세요.",
                                        "createdAt": "2026-08-14T18:00:00",
                                        "updatedAt": "2026-08-18T09:00:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 팀 관행을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "해당 팀 관행을 찾을 수 없습니다. normId=1",
                                      "path": "/api/v1/workspaces/1/norms/1"
                                    }"""))),
    })
    @PutMapping("/{normId}")
    public ResponseEntity<?> updateNorm(
            @PathVariable Long workspaceId,
            @PathVariable Long normId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(schema = @Schema(implementation = TeamNormRequestDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "category": "COMMUNICATION",
                                      "content": "오후 6시 이후의 급한 연락은 슬랙 멘션 대신 이메일로 남겨주세요."
                                    }"""))
            )
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "팀 관행 삭제 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "code": "TEAM_NORM_DELETED",
                              "message": "팀 관행 삭제 성공"
                            }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "해당 팀 관행을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 404,
                                      "error": "Not Found",
                                      "message": "해당 팀 관행을 찾을 수 없습니다. normId=1",
                                      "path": "/api/v1/workspaces/1/norms/1"
                                    }"""))),
    })
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