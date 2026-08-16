package com.hufsphere.linkboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_norms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamNorm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(nullable = false, length = 100)
    private String category; // 예: COMMUNICATION, CODE_REVIEW, MEETING, CONVENTION 등

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String category, String content) {
        if (category != null && !category.isBlank()) {
            this.category = category;
        }
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
        this.updatedAt = LocalDateTime.now();
    }
}