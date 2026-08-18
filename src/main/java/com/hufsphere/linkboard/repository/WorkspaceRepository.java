package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.Workspace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findByInviteCode(String inviteCode);
}
