package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.common.ApiResponse;
import com.hufsphere.linkboard.common.ErrorResponse;
import com.hufsphere.linkboard.dto.DashboardFeaturesResponse;
import com.hufsphere.linkboard.dto.DashboardSourcesResponse;
import com.hufsphere.linkboard.dto.TeamDashboardResponse;
import com.hufsphere.linkboard.dto.WorkItemDetailResponse;
import com.hufsphere.linkboard.dto.WorkItemPageResponse;
import com.hufsphere.linkboard.dto.WorkItemSummaryResponse;
import com.hufsphere.linkboard.service.WorkItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "WorkItem", description = "작업 상세, 요약, 목록 및 팀 현황판 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkItemController {

    private final WorkItemService workItemService;

    @Operation(summary = "작업 상세 조회", description = "특정 작업의 상세 정보 및 관련 연결 항목 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "작업 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = WorkItemDetailResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "WORK_ITEM_OK",
                                      "message": "작업 상세 조회 성공",
                                      "data": {
                                        "id": 1,
                                        "sourceType": "github",
                                        "itemType": "pr",
                                        "sourceNumber": 142,
                                        "title": "Add JWT auth",
                                        "status": "done",
                                        "statusLabel": "완료",
                                        "authorLogin": "jaeyoung123",
                                        "sourceUrl": "https://github.com/org/repo/pull/142",
                                        "sourceUpdatedAt": "2026-08-09T15:12:40",
                                        "summaryNative": "JWT 기반 인증 도입 PR",
                                        "linkedItems": [
                                          {
                                            "id": 2,
                                            "sourceType": "notion",
                                            "itemType": "page",
                                            "title": "인증 방식 논의",
                                            "sourceUrl": "https://notion.so/auth-discussion"
                                          }
                                        ]
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "작업을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "작업을 찾을 수 없습니다. id=1",
                                      "path": "/api/v1/work-items/1"
                                    }"""))),
    })
    @GetMapping("/work-items/{workItemId}")
    public ResponseEntity<?> getWorkItemDetail(
            @PathVariable Long workItemId,
            @Parameter(description = "statusLabel 언어. 없으면 워크스페이스 기본 언어 (ko가 아니면 영어 라벨)", example = "ko")
            @RequestParam(required = false) String lang
    ) {
        WorkItemDetailResponse detail = workItemService.getWorkItemDetail(workItemId, lang);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "WORK_ITEM_OK",
                "message", "작업 상세 조회 성공",
                "data", detail
        ));
    }

    @Operation(summary = "가지 요약 조회", description = "특정 작업의 지정된 언어 요약을 조회하거나 없으면 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "요약 조회 성공",
                    content = @Content(schema = @Schema(implementation = WorkItemSummaryResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "SUMMARY_OK",
                                      "message": "요약 조회 성공",
                                      "data": {
                                        "workItemId": 1,
                                        "lang": "ko",
                                        "summaryText": "JWT 기반 인증 도입 PR",
                                        "createdAt": "2026-08-18T15:10:00"
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "lang 누락/미지원 또는 작업을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "timestamp": "2026-08-18T15:10:00",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "lang은 필수이며 ko, vi, en 중 하나여야 합니다",
                                      "path": "/api/v1/work-items/1/summary"
                                    }"""))),
    })
    @GetMapping("/work-items/{workItemId}/summary")
    public ResponseEntity<?> getWorkItemSummary(
            @PathVariable Long workItemId,
            @RequestParam String lang
    ) {
        if (lang == null || (!lang.equals("ko") && !lang.equals("vi") && !lang.equals("en"))) {
            return ResponseEntity.badRequest().body(Map.of(
                    "timestamp", java.time.LocalDateTime.now().toString(),
                    "status", 400,
                    "error", "Bad Request",
                    "message", "lang은 필수이며 ko, vi, en 중 하나여야 합니다",
                    "path", "/api/v1/work-items/" + workItemId + "/summary"
            ));
        }

        WorkItemSummaryResponse summary = workItemService.getWorkItemSummary(workItemId, lang);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "SUMMARY_OK",
                "message", "요약 조회 성공",
                "data", summary
        ));
    }

    @Operation(summary = "작업 목록 조회", description = "워크스페이스 내 작업 항목들을 검색, 필터링하여 목록으로 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "작업 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = WorkItemPageResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "WORK_ITEM_LIST_OK",
                                      "message": "작업 목록 조회 성공",
                                      "data": {
                                        "items": [
                                          {
                                            "id": 1,
                                            "sourceType": "github",
                                            "itemType": "pr",
                                            "sourceNumber": 142,
                                            "title": "Add JWT auth",
                                            "status": "done",
                                            "statusLabel": "완료",
                                            "authorLogin": "jaeyoung123",
                                            "sourceUrl": "https://github.com/org/repo/pull/142",
                                            "sourceUpdatedAt": "2026-08-09T15:12:40"
                                          }
                                        ],
                                        "page": 1,
                                        "size": 20,
                                        "totalElements": 1,
                                        "totalPages": 1
                                      }
                                    }"""))),
    })
    @GetMapping("/workspaces/{workspaceId}/work-items")
    public ResponseEntity<?> getWorkItems(
            @PathVariable Long workspaceId,
            @Parameter(description = "제목 검색어", example = "auth")
            @RequestParam(required = false) String query,
            @Parameter(description = "소스 필터 (github/notion/figma)", example = "github")
            @RequestParam(required = false) String sourceType,
            @Parameter(description = "상태 필터 (todo/in_progress/review/done)", example = "todo")
            @RequestParam(required = false) String status,
            @Parameter(description = "정렬 기준", example = "sourceUpdatedAt,desc")
            @RequestParam(defaultValue = "sourceUpdatedAt,desc") String sort,
            @Parameter(description = "페이지 번호(1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "statusLabel 등 표시 문구 언어. 없으면 워크스페이스 기본 언어 (ko가 아니면 영어 라벨)", example = "ko")
            @RequestParam(required = false) String lang
    ) {
        WorkItemPageResponse response = workItemService.getWorkItems(workspaceId, query, sourceType, status, sort, page, size, lang);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "WORK_ITEM_LIST_OK",
                "message", "작업 목록 조회 성공",
                "data", response
        ));
    }

    @Operation(summary = "팀 현황판 조회", description = "워크스페이스 내 전체 작업 현황 및 멤버별 작업 통계를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "팀 현황판 조회 성공",
                    content = @Content(schema = @Schema(implementation = TeamDashboardResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "DASHBOARD_OK",
                                      "message": "팀 현황판 조회 성공",
                                      "data": {
                                        "totalWorkItems": 12,
                                        "statusCounts": {
                                          "todo": 5,
                                          "in_progress": 3,
                                          "review": 1,
                                          "done": 3
                                        },
                                        "members": [
                                          {
                                            "authorLogin": "jaeyoung123",
                                            "totalAssigned": 4,
                                            "statusCounts": {
                                              "todo": 2,
                                              "done": 2
                                            }
                                          }
                                        ]
                                      }
                                    }"""))),
    })
    @GetMapping("/workspaces/{workspaceId}/team-dashboard")
    public ResponseEntity<?> getTeamDashboard(@PathVariable Long workspaceId) {
        TeamDashboardResponse response = workItemService.getTeamDashboard(workspaceId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "DASHBOARD_OK",
                "message", "팀 현황판 조회 성공",
                "data", response
        ));
    }

    @Operation(summary = "기능별 진행률 조회", description = "AI가 group-features로 분류한 기능 중 가장 최근에 업데이트된 상위 3개(A/B/C)의 진행률을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "기능별 진행률 조회 성공",
                    content = @Content(schema = @Schema(implementation = DashboardFeaturesResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "DASHBOARD_FEATURES_OK",
                                      "message": "기능별 진행률 조회 성공",
                                      "data": {
                                        "features": [
                                          {
                                            "featureId": 1,
                                            "name": "인증/로그인",
                                            "totalCount": 5,
                                            "doneCount": 3,
                                            "progress": 0.6,
                                            "lastUpdatedAt": "2026-08-18T09:00:00"
                                          }
                                        ]
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/workspaces/{workspaceId}/dashboard/features")
    public ResponseEntity<ApiResponse<DashboardFeaturesResponse>> getFeatureDashboard(@PathVariable Long workspaceId) {
        DashboardFeaturesResponse response = workItemService.getFeatureProgress(workspaceId);
        return ResponseEntity.ok(ApiResponse.success("DASHBOARD_FEATURES_OK", "기능별 진행률 조회 성공", response));
    }

    @Operation(summary = "소스별 현황 조회", description = "워크스페이스에 연동된 Figma/Github/Notion 소스별 상태·진행률·최근 이슈를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "소스별 현황 조회 성공",
                    content = @Content(schema = @Schema(implementation = DashboardSourcesResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "code": "DASHBOARD_SOURCES_OK",
                                      "message": "소스별 현황 조회 성공",
                                      "data": {
                                        "sources": [
                                          {
                                            "sourceId": 1,
                                            "sourceType": "github",
                                            "sourceRef": "pypa/sampleproject",
                                            "connStatus": "CONNECTED",
                                            "totalCount": 30,
                                            "doneCount": 18,
                                            "progress": 0.6,
                                            "recentIssues": [
                                              {
                                                "workItemId": 142,
                                                "title": "Add JWT auth",
                                                "status": "done",
                                                "sourceUrl": "https://github.com/org/repo/pull/142",
                                                "sourceUpdatedAt": "2026-08-18T09:00:00"
                                              }
                                            ]
                                          }
                                        ]
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "워크스페이스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/workspaces/{workspaceId}/dashboard/sources")
    public ResponseEntity<ApiResponse<DashboardSourcesResponse>> getSourceDashboard(@PathVariable Long workspaceId) {
        DashboardSourcesResponse response = workItemService.getSourceProgress(workspaceId);
        return ResponseEntity.ok(ApiResponse.success("DASHBOARD_SOURCES_OK", "소스별 현황 조회 성공", response));
    }
}