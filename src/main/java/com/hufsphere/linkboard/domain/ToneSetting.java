package com.hufsphere.linkboard.domain;

import com.hufsphere.linkboard.common.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tone_setting", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tone_setting_user", columnNames = {"user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ToneSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // concise/detailed/friendly 중 0개 이상 중복 선택 가능. 아무것도 선택하지 않은 "선택 안 함"
    // 상태도 유효하므로(customText만으로 저장 가능) 빈 리스트를 그대로 허용한다.
    @Convert(converter = StringListConverter.class)
    @Column(name = "preset_keys", nullable = false, length = 100)
    private List<String> presetKeys;

    @Column(name = "custom_text", columnDefinition = "TEXT")
    private String customText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ToneSetting(Long userId, List<String> presetKeys, String customText) {
        this.userId = userId;
        this.presetKeys = presetKeys != null ? presetKeys : List.of();
        this.customText = customText;
    }

    public void update(List<String> presetKeys, String customText) {
        this.presetKeys = presetKeys != null ? presetKeys : List.of();
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
