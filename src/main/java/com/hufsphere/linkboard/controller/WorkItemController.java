package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.dto.WorkItemDetailResponse;
import com.hufsphere.linkboard.dto.WorkItemPageResponse;
import com.hufsphere.linkboard.dto.WorkItemSummaryResponse;
import com.hufsphere.linkboard.service.WorkItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "WorkItem", description = "작업 상세, 요약 및 목록 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkItemController {

    private final WorkItemService workItemService;

    @Operation(summary = "작업 상세 조회", description = "특정 작업의 상세 정보 및 관련 연결 항목 목록을 조회합니다.")
    @GetMapping("/work-items/{workItemId}")
    public ResponseEntity<?> getWorkItemDetail(
            @PathVariable Long workItemId,
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
    @GetMapping("/workspaces/{workspaceId}/work-items")
    public ResponseEntity<?> getWorkItems(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "sourceUpdatedAt,desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        WorkItemPageResponse response = workItemService.getWorkItems(workspaceId, query, sourceType, status, sort, page, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "code", "WORK_ITEM_LIST_OK",
                "message", "작업 목록 조회 성공",
                "data", response
        ));
    }
}