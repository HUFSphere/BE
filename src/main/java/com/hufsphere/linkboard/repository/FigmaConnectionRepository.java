package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.FigmaConnection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FigmaConnectionRepository extends JpaRepository<FigmaConnection, Long> {

    // 재연결 시 기존 행을 갱신하지 않고 새로 insert하므로, 가장 최근(=유효한) 토큰을 사용한다.
    Optional<FigmaConnection> findFirstByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
}
