package com.hufsphere.linkboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tone_setting", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tone_setting_workspace_user", columnNames = {"workspace_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ToneSetting {

    public static final String DEFAULT_PRESET_KEY = "beginner";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "preset_key", nullable = false, length = 20)
    private String presetKey;

    @Column(name = "custom_text", columnDefinition = "TEXT")
    private String customText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ToneSetting(Long workspaceId, Long userId, String presetKey, String customText) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.presetKey = (presetKey != null && !presetKey.isBlank()) ? presetKey : DEFAULT_PRESET_KEY;
        this.customText = customText;
    }

    public void update(String presetKey, String customText) {
        this.presetKey = presetKey;
        this.customText = customText;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
