package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.Feature;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRepository extends JpaRepository<Feature, Long> {

    List<Feature> findByWorkspaceId(Long workspaceId);

    void deleteByWorkspaceId(Long workspaceId);
}
