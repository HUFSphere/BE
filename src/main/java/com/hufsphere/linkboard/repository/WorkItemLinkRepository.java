package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.WorkItemLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkItemLinkRepository extends JpaRepository<WorkItemLink, Long> {

    List<WorkItemLink> findByFromWorkItemWorkspaceId(Long workspaceId);

    // 특정 workItemId가 출발(from) 또는 도착(to)인 모든 연결 조회
    List<WorkItemLink> findByFromWorkItemIdOrToWorkItemId(Long fromId, Long toId);
}