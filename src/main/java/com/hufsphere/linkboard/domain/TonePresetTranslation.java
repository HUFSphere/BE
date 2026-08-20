package com.hufsphere.linkboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// ko/en 외의 언어로 요청된 톤 프리셋(concise/detailed/friendly) 라벨·설명 번역 결과 캐시.
// AI 서버 호출은 언어당 최초 1회뿐이고, 그 뒤로는 이 테이블에서 즉시 반환한다.
@Entity
@Table(name = "tone_preset_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lang", "preset_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class TonePresetTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String lang;

    @Column(name = "preset_key", nullable = false, length = 50)
    private String presetKey;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
