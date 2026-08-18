package com.hufsphere.linkboard.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_language", length = 10)
    @Builder.Default
    private String defaultLanguage = "ko";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private AppUser owner;

    @Column(name = "invite_code", unique = true, length = 20)
    private String inviteCode;

    @Column(name = "invite_code_expires_at")
    private LocalDateTime inviteCodeExpiresAt;

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

    /**
     * 초대 코드 및 만료일시 갱신 비즈니스 메서드
     */
    public void updateInviteCode(String newInviteCode, LocalDateTime expiresAt) {
        this.inviteCode = newInviteCode;
        this.inviteCodeExpiresAt = expiresAt;
        this.updatedAt = LocalDateTime.now();
    }
}