package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.dto.TonePresetItemDto;
import com.hufsphere.linkboard.client.dto.TranslateTonePresetsResponse;
import com.hufsphere.linkboard.common.TonePresets;
import com.hufsphere.linkboard.domain.TonePresetTranslation;
import com.hufsphere.linkboard.repository.TonePresetTranslationRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

// TonePresets(ko/en 하드코딩)에 없는 언어를 요청하면 AI 서버로 실시간 번역해서 DB에 캐싱한다.
// 같은 언어의 다음 요청부터는 AI 호출 없이 캐시에서 즉시 반환된다. 번역 API 호출은 DB 트랜잭션
// 밖에서 수행해서(느린 외부 호출 동안 커넥션/락을 붙들지 않도록) 저장만 짧게 별도로 처리한다.
@Service
@RequiredArgsConstructor
public class TonePresetTranslationService {

    private static final Set<String> BUILT_IN_LANGS = Set.of("ko", "en");

    private final TonePresetTranslationRepository translationRepository;
    private final AiServerClient aiServerClient;

    public List<TonePresets.TonePreset> resolvePresets(String lang) {
        if (lang == null || BUILT_IN_LANGS.contains(lang)) {
            return TonePresets.getPresets(lang);
        }

        List<TonePresetTranslation> cached = translationRepository.findByLang(lang);
        if (!cached.isEmpty()) {
            return toPresets(cached);
        }

        return translateAndCache(lang);
    }

    public String resolveDescription(String presetKey, String lang) {
        if (lang == null || BUILT_IN_LANGS.contains(lang)) {
            return TonePresets.getDescription(presetKey, lang);
        }

        return resolvePresets(lang).stream()
                .filter(preset -> preset.presetKey().equals(presetKey))
                .map(TonePresets.TonePreset::description)
                .findFirst()
                .orElseGet(() -> TonePresets.getDescription(presetKey, "en"));
    }

    private List<TonePresets.TonePreset> translateAndCache(String lang) {
        List<TonePresetItemDto> englishItems = TonePresets.getPresets("en").stream()
                .map(preset -> new TonePresetItemDto(preset.presetKey(), preset.label(), preset.description()))
                .toList();

        TranslateTonePresetsResponse response = aiServerClient.translateTonePresets(lang, englishItems);

        List<TonePresetTranslation> toSave = response.getPresets().stream()
                .map(item -> TonePresetTranslation.builder()
                        .lang(lang)
                        .presetKey(item.getPresetKey())
                        .label(item.getLabel())
                        .description(item.getDescription())
                        .build())
                .toList();

        try {
            return toPresets(translationRepository.saveAll(toSave));
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 언어를 처음 요청한 다른 스레드가 먼저 저장을 끝낸 경우(lang+presetKey
            // 유니크 제약 위반) — 새로 번역하는 대신 그 결과를 그대로 읽어와 반환한다.
            return toPresets(translationRepository.findByLang(lang));
        }
    }

    private List<TonePresets.TonePreset> toPresets(List<TonePresetTranslation> translations) {
        return translations.stream()
                .map(t -> new TonePresets.TonePreset(t.getPresetKey(), t.getLabel(), t.getDescription()))
                .toList();
    }
}
