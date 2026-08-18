package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.NotionConnection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotionConnectionRepository extends JpaRepository<NotionConnection, Long> {

    // 재연결 시 기존 행을 갱신하지 않고 새로 insert하므로, 가장 최근(=유효한) 토큰을 사용한다.
    Optional<NotionConnection> findFirstByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
}
