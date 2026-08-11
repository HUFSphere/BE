package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.SourceConnection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceConnectionRepository extends JpaRepository<SourceConnection, Long> {

    List<SourceConnection> findByWorkspaceIdOrderByCreatedAtAsc(Long workspaceId);
}
