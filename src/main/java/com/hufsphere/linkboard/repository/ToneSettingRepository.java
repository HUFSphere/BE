package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.ToneSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToneSettingRepository extends JpaRepository<ToneSetting, Long> {

    Optional<ToneSetting> findByUserId(Long userId);
}
