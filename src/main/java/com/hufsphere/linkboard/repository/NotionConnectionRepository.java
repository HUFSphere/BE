package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.NotionConnection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotionConnectionRepository extends JpaRepository<NotionConnection, Long> {
}
