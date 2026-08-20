package com.hufsphere.linkboard.repository;

import com.hufsphere.linkboard.domain.TonePresetTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TonePresetTranslationRepository extends JpaRepository<TonePresetTranslation, Long> {
    List<TonePresetTranslation> findByLang(String lang);
}
