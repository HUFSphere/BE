package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.SourceConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceConnectionRepository extends JpaRepository<SourceConnection, Long> {
    List<SourceConnection> findByWorkspaceId(Long workspaceId);
}