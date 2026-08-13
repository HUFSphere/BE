package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    List<WorkItem> findByWorkspaceId(Long workspaceId);
    List<WorkItem> findByWorkspaceIdAndSourceType(Long workspaceId, SourceType sourceType);
}