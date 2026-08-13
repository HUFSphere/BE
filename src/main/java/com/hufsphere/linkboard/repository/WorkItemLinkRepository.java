package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.WorkItemLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkItemLinkRepository extends JpaRepository<WorkItemLink, Long> {

    List<WorkItemLink> findByFromWorkItemWorkspaceId(Long workspaceId);

    // 특정 workItemId가 출발(from) 또는 도착(to)인 모든 연결 조회
    List<WorkItemLink> findByFromWorkItemIdOrToWorkItemId(Long fromId, Long toId);

    // 재동기화 시 이 소스에 속한 work_item이 얽힌 연결을 먼저 정리하기 위함 (work_item 삭제보다 선행되어야 함)
    @Modifying
    @Query("DELETE FROM WorkItemLink l WHERE l.fromWorkItem.sourceConnection.id = :sourceConnectionId "
            + "OR l.toWorkItem.sourceConnection.id = :sourceConnectionId")
    void deleteBySourceConnectionId(@Param("sourceConnectionId") Long sourceConnectionId);
}