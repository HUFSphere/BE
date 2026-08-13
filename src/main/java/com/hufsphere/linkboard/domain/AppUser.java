package com.hufsphere.linkboard.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_app_user_oauth",
                        columnNames = {"oauth_provider", "oauth_subject"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String name;

    @Column(name = "oauth_provider", nullable = false, length = 20)
    private String oauthProvider;

    @Column(name = "oauth_subject", nullable = false, length = 255)
    private String oauthSubject;

    @Column(name = "native_lang", nullable = false, length = 10)
    private String nativeLang = "ko";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AppUser(
            String name,
            String oauthProvider,
            String oauthSubject,
            String nativeLang
    ) {
        this.name = name;
        this.oauthProvider = oauthProvider;
        this.oauthSubject = oauthSubject;
        this.nativeLang = nativeLang;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.nativeLang == null) {
            this.nativeLang = "ko";
        }
    }
}