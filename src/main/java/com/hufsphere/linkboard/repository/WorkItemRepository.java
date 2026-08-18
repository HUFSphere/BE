package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {

    // 기본 워크스페이스별 조회
    List<WorkItem> findByWorkspaceId(Long workspaceId);

    // MapService에서 사용하는 sourceType별 조회
    List<WorkItem> findByWorkspaceIdAndSourceType(Long workspaceId, SourceType sourceType);

    // 5.7 대시보드 최근 활동 조회용
    List<WorkItem> findTop3ByWorkspaceIdOrderBySourceUpdatedAtDesc(Long workspaceId);

    // develop 브랜치 연동 메서드
    List<WorkItem> findBySourceConnectionId(Long sourceConnectionId);
    void deleteBySourceConnectionId(Long sourceConnectionId);
    void deleteByWorkspaceId(Long workspaceId);
    long countBySourceConnectionId(Long sourceConnectionId);

    // 5.4 플랫폼(sourceType) 및 상태(status) 필터링 조회 쿼리
    @Query("SELECT w FROM WorkItem w WHERE w.workspace.id = :workspaceId " +
            "AND (:sourceType IS NULL OR UPPER(STR(w.sourceType)) = UPPER(:sourceType)) " +
            "AND (:status IS NULL OR UPPER(STR(w.status)) = UPPER(:status)) " +
            "ORDER BY w.sourceUpdatedAt DESC")
    List<WorkItem> findByWorkspaceIdAndFilters(
            @Param("workspaceId") Long workspaceId,
            @Param("sourceType") String sourceType,
            @Param("status") String status
    );
}