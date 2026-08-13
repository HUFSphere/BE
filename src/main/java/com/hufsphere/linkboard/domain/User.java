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

// 일반 회원가입(username+password)과 소셜 로그인(oauthProvider+oauthSubject) 두 가입 경로를
// 하나의 계정 테이블로 수용한다. 소셜 로그인(1.0/1.1)은 다른 팀원이 별도로 구현 중 —
// username/password는 그쪽 계정에서 비어있을 수 있어 nullable로 둔다.
@Entity
@Table(name = "app_user", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_user_oauth", columnNames = {"oauth_provider", "oauth_subject"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private static final String DEFAULT_NATIVE_LANG = "ko";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 일반 회원가입 전용 (소셜 로그인 전용 계정이면 null)
    @Column(unique = true, length = 20)
    private String username;

    // 해시된 비밀번호만 저장 (BCrypt). 소셜 로그인 전용 계정이면 null
    @Column(length = 100)
    private String password;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "native_lang", nullable = false, length = 10)
    private String nativeLang;

    // 소셜 로그인 전용 (일반 회원가입 계정이면 null). github/google/kakao
    @Column(name = "oauth_provider", length = 20)
    private String oauthProvider;

    // 제공자별 사용자 고유 식별자 (oauthProvider + oauthSubject 조합으로 사용자 식별)
    @Column(name = "oauth_subject", length = 100)
    private String oauthSubject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String username, String password, String name, String nativeLang,
            String oauthProvider, String oauthSubject) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.nativeLang = (nativeLang != null && !nativeLang.isBlank()) ? nativeLang : DEFAULT_NATIVE_LANG;
        this.oauthProvider = oauthProvider;
        this.oauthSubject = oauthSubject;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
