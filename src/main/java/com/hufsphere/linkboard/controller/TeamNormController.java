package com.hufsphere.linkboard.controller;

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

@Tag(name = "TeamNorms", description = "팀 관행(Team Norms) 조회 API (6번). GitHub/Notion/Figma 동기화 내용을 AI가 " +
        "분석해서 자동으로 채워지며, 소스 동기화마다 워크스페이스 단위로 재생성된다. 수동 등록/수정/삭제는 지원하지 않는다.")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/norms")
@RequiredArgsConstructor
public class TeamNormController {

    private final TeamNormService teamNormService;

    @Operation(
            summary = "팀 관행 목록 조회 (6.1)",
            description = "워크스페이스의 팀 관행 목록을 조회합니다. 각 항목은 실제 기록에서 관찰된 패턴을 요약한 1문장(content)과 " +
                    "그 근거가 되는 기록 링크 1개(evidenceUrl)로 구성됩니다. 아직 소스 동기화가 한 번도 안 됐거나, 반복 패턴이 " +
                    "발견되지 않았으면 빈 배열을 반환합니다."
    )
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
                                  "category": "CODE_REVIEW",
                                  "content": "최근 PR 12건 중 10건이 리뷰어 2명의 승인을 받은 뒤 머지되었습니다.",
                                  "evidenceUrl": "https://github.com/HUFSphere/BE/pull/16",
                                  "evidenceTitle": "feat: 인증·워크스페이스·소스 연동 반영",
                                  "evidenceSourceType": "github",
                                  "createdAt": "2026-08-20T18:00:00"
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
}
