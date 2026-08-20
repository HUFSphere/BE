package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.SourceConnection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SourceConnectionRepository
        extends JpaRepository<SourceConnection, Long> {

    List<SourceConnection> findByWorkspaceId(
            Long workspaceId
    );

    long countByWorkspaceId(
            Long workspaceId
    );

    void deleteAllByWorkspaceId(
            Long workspaceId
    );

    // 동시에 여러 sync 요청이 들어와도 "SYNCING이 아닐 때만 SYNCING으로 바꾼다"를 DB 레벨에서
    // 원자적으로 처리한다. 읽기(findById로 상태 확인) 후 쓰기(save)를 따로 하면 그 사이에 다른
    // 요청이 끼어들 수 있어 동시 요청이 전부 통과하는 레이스 컨디션이 생긴다.
    // 반환값이 0이면 이미 다른 요청이 SYNCING을 선점했다는 뜻이다. 별도 트랜잭션으로 짧게 끝나야
    // 하므로(뒤이어 오래 걸리는 외부 API 호출 동안 락을 들고 있지 않도록) 이 메서드 자체에
    // @Transactional을 둔다.
    @Transactional
    @Modifying
    @Query("UPDATE SourceConnection sc SET sc.status = 'SYNCING' WHERE sc.id = :id AND sc.status <> 'SYNCING'")
    int markSyncing(@Param("id") Long id);
}