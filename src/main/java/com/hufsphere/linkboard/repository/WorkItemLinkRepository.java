package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.WorkItemLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkItemLinkRepository extends JpaRepository<WorkItemLink, Long> {

    // from/to 둘 다 이 워크스페이스에 속한 work_item인 링크만 (한쪽만 걸리는 경우 제외)
    List<WorkItemLink> findByFromWorkItemWorkspaceIdAndToWorkItemWorkspaceId(Long fromWorkspaceId, Long toWorkspaceId);

    // 특정 workItemId가 출발(from) 또는 도착(to)인 모든 연결 조회
    List<WorkItemLink> findByFromWorkItemIdOrToWorkItemId(Long fromId, Long toId);

    // 재동기화 시 이 소스에 속한 work_item이 얽힌 연결을 먼저 정리하기 위함 (work_item 삭제보다 선행되어야 함)
    @Modifying
    @Query("DELETE FROM WorkItemLink l WHERE l.fromWorkItem.sourceConnection.id = :sourceConnectionId "
            + "OR l.toWorkItem.sourceConnection.id = :sourceConnectionId")
    void deleteBySourceConnectionId(@Param("sourceConnectionId") Long sourceConnectionId);

    // AI의 /extract-work-items·/link-work-items는 소스 하나가 아니라 워크스페이스 전체 색인을 대상으로
    // 응답하므로(다중 소스 간 링크 포함), 재동기화 시에는 워크스페이스 단위로 통째로 갈아끼운다.
    @Modifying
    @Query("DELETE FROM WorkItemLink l WHERE l.fromWorkItem.workspace.id = :workspaceId "
            + "OR l.toWorkItem.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") Long workspaceId);
}