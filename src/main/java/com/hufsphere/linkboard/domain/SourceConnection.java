package com.hufsphere.linkboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "source_connections")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SourceConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private String status;

    private String targetRepoOrBoard;

    private LocalDateTime lastSyncedAt;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "CONNECTED";
        }
    }

    // --- SourceSyncService에서 필요한 편의 메서드들 ---

    public String getSourceRef() {
        return this.targetRepoOrBoard;
    }

    public boolean isSyncInProgress() {
        return "SYNCING".equalsIgnoreCase(this.status);
    }

    public void startSyncing() {
        this.status = "SYNCING";
    }

    public void failSyncing() {
        this.status = "FAILED";
    }

    public void completeSyncing(LocalDateTime syncedAt) {
        this.status = "CONNECTED";
        this.lastSyncedAt = syncedAt;
    }
}