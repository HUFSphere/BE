package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.MapResponse;
import com.hufsphere.linkboard.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Map", description = "프로젝트 지도 API")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @Operation(summary = "프로젝트 지도 조회", description = "워크스페이스의 work_item(노드)과 work_item_link(연결)를 조회한다. 좌표/레이아웃 계산은 프론트엔드 몫이며 BE는 노드·링크 데이터만 내려준다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 지도 조회 성공",
                    content = @Content(schema = @Schema(implementation = MapResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "MAP_OK",
                                      "message": "프로젝트 지도 조회 성공",
                                      "data": {
                                        "nodes": [
                                          {
                                            "id": 1,
                                            "sourceType": "github",
                                            "itemType": "pr",
                                            "sourceNumber": 142,
                                            "title": "Add JWT auth",
                                            "status": "done",
                                            "statusLabel": "완료",
                                            "summaryBrief": "JWT 기반 인증 도입",
                                            "authorLogin": "jaeyoung123",
                                            "sourceUrl": "https://github.com/org/repo/pull/142",
                                            "sourceUpdatedAt": "2026-08-09T15:12:40"
                                          }
                                        ],
                                        "links": [
                                          {
                                            "fromWorkItemId": 1,
                                            "toWorkItemId": 2,
                                            "linkSource": "ai",
                                            "linkReason": "동일한 인증 기능을 다룸"
                                          }
                                        ]
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
                                      "message": "워크스페이스를 찾을 수 없습니다",
                                      "path": "/api/v1/workspaces/1/map"
                                    }"""))),
    })
    @GetMapping
    public ResponseEntity<ApiResponse<MapResponse>> getProjectMap(
            @PathVariable Long workspaceId,
            @Parameter(description = "statusLabel 언어. 없으면 워크스페이스 기본 언어 (ko가 아니면 영어 라벨)") @RequestParam(required = false) String lang,
            @Parameter(description = "소스 필터 (github/notion/figma). 없으면 전체") @RequestParam(required = false) String sourceType
    ) {
        MapResponse mapResponse = mapService.getProjectMap(workspaceId, lang, sourceType);
        return ResponseEntity.ok(ApiResponse.success("MAP_OK", "프로젝트 지도 조회 성공", mapResponse));
    }
}