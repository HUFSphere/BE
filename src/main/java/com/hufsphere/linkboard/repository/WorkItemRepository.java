package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    List<WorkItem> findByWorkspaceId(Long workspaceId);
    List<WorkItem> findByWorkspaceIdAndSourceType(Long workspaceId, SourceType sourceType);
    List<WorkItem> findBySourceConnectionId(Long sourceConnectionId);
    void deleteBySourceConnectionId(Long sourceConnectionId);
    void deleteByWorkspaceId(Long workspaceId);
    long countBySourceConnectionId(Long sourceConnectionId);
    // 가장 최근에 갱신된 작업 3개 조회
    List<WorkItem> findTop3ByWorkspaceIdOrderBySourceUpdatedAtDesc(Long workspaceId);
}