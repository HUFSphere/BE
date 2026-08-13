package com.hufsphere.linkboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 일반 회원가입(username+password, 1.6/1.7)과 소셜 로그인(oauthProvider+oauthSubject, 1.0/1.1)
// 두 가입 경로를 하나의 계정 테이블로 수용한다. 각 경로 전용 컬럼은 반대쪽 경로로 만든
// 계정에서는 비어있을 수 있어 nullable로 둔다.

@Entity
@Table(name = "app_user", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_user_oauth", columnNames = {"oauth_provider", "oauth_subject"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

    private static final String DEFAULT_NATIVE_LANG = "ko";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String username;

    @Column(length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "native_lang", nullable = false, length = 10)
    private String nativeLang;

    @Column(name = "oauth_provider", length = 20)
    private String oauthProvider;

    @Column(name = "oauth_subject", length = 255)
    private String oauthSubject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private AppUser(
            String username,
            String password,
            String name,
            String nativeLang,
            String oauthProvider,
            String oauthSubject
    ) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.nativeLang =
                (nativeLang != null && !nativeLang.isBlank())
                        ? nativeLang
                        : DEFAULT_NATIVE_LANG;
        this.oauthProvider = oauthProvider;
        this.oauthSubject = oauthSubject;
    }

    public void updateProfile(String name, String nativeLang) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }

        if (nativeLang != null && !nativeLang.isBlank()) {
            this.nativeLang = nativeLang;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}