package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.TeamNorm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamNormRepository extends JpaRepository<TeamNorm, Long> {
    List<TeamNorm> findByWorkspaceIdOrderByIdAsc(Long workspaceId);
    void deleteByWorkspaceId(Long workspaceId);
}
