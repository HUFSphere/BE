package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.TeamNorm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamNormRepository extends JpaRepository<TeamNorm, Long> {
    List<TeamNorm> findByWorkspaceIdOrderByIdAsc(Long workspaceId);
    Optional<TeamNorm> findByIdAndWorkspaceId(Long id, Long workspaceId);
}