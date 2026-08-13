package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByOauthProviderAndOauthSubject(
            String oauthProvider,
            String oauthSubject
    );
}