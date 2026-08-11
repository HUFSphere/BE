package com.hufsphere.linkboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "source_connection")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SourceConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Column(name = "source_ref", nullable = false, length = 500)
    private String sourceRef;

    @Column(name = "conn_status", nullable = false, length = 20)
    private ConnStatus connStatus;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private SourceConnection(Long workspaceId, SourceType sourceType, String sourceRef) {
        this.workspaceId = workspaceId;
        this.sourceType = sourceType;
        this.sourceRef = sourceRef;
        this.connStatus = ConnStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isSyncInProgress() {
        return connStatus == ConnStatus.COLLECTING
                || connStatus == ConnStatus.INDEXING
                || connStatus == ConnStatus.SUMMARIZING;
    }

    public void startSyncing() {
        this.connStatus = ConnStatus.COLLECTING;
    }

    public void completeSyncing(LocalDateTime syncedAt) {
        this.connStatus = ConnStatus.DONE;
        this.lastSyncedAt = syncedAt;
    }

    public void failSyncing() {
        this.connStatus = ConnStatus.FAILED;
    }
}
