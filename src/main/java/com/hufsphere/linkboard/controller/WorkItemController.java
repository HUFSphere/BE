package com.hufsphere.linkboard.controller;

import com.hufsphere.linkboard.dto.WorkItemDetailResponse;
import com.hufsphere.linkboard.service.WorkItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "WorkItem", description = "작업 상세 API")
@RestController
@RequestMapping("/api/v1/work-items")
@RequiredArgsConstructor
public class WorkItemController {

    private final WorkItemService workItemService;

    @Operation(summary = "작업 상세 조회", description = "특정 작업의 상세 정보 및 관련 연결 항목 목록을 조회합니다.")
    @GetMapping("/{workItemId}")
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
}