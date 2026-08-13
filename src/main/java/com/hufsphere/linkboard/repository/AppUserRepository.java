package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<AppUser> findByOauthProviderAndOauthSubject(String oauthProvider, String oauthSubject);
}
