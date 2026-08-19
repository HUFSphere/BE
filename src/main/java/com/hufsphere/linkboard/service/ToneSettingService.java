package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.common.LanguageScriptDetector;
import com.hufsphere.linkboard.common.TonePresets;
import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.domain.ToneSetting;
import com.hufsphere.linkboard.dto.request.ToneSettingRequest;
import com.hufsphere.linkboard.dto.response.ToneSettingResponse;
import com.hufsphere.linkboard.dto.response.TonePresetResponse;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.repository.ToneSettingRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ToneSettingService {

    private final ToneSettingRepository toneSettingRepository;
    private final AppUserRepository appUserRepository;

    // 1. 톤 프리셋 목록 조회. lang이 없으면 요청자의 native_lang을 사용한다.
    public List<TonePresetResponse> getPresets(Long requesterId, String lang) {
        String resolvedLang = (lang != null && !lang.isBlank()) ? lang : resolveRequesterNativeLang(requesterId);

        return TonePresets.getPresets(resolvedLang).stream()
                .map(TonePresetResponse::from)
                .toList();
    }

    // 3. 톤 설정 조회. 저장된 설정이 없으면 기본값(beginner)을 에러 없이 반환한다.
    // 워크스페이스 단위가 아니라 사용자 개인 단위 설정이라, 로그인만 되어 있으면(컨트롤러에서 검증)
    // 워크스페이스 소속 여부와 무관하게 조회/저장할 수 있다.
    public ToneSettingResponse getToneSetting(Long userId) {
        return toneSettingRepository.findByUserId(userId)
                .map(ToneSettingResponse::from)
                .orElseGet(ToneSettingResponse::defaultResponse);
    }

    // 2. 톤 설정 저장(upsert). beginner/intermediate/expert 중 하나 이상 중복 선택 가능
    @Transactional
    public ToneSettingResponse saveToneSetting(Long userId, ToneSettingRequest request) {
        List<String> presetKeys = request.getPresetKeys().stream().distinct().toList();
        for (String presetKey : presetKeys) {
            if (!TonePresets.isValidPresetKey(presetKey)) {
                throw new IllegalArgumentException("presetKeys는 beginner/intermediate/expert로만 구성되어야 합니다");
            }
        }

        ToneSetting toneSetting = toneSettingRepository.findByUserId(userId)
                .orElseGet(() -> ToneSetting.builder()
                        .userId(userId)
                        .build());

        toneSetting.update(presetKeys, request.getCustomText());

        ToneSetting saved = toneSettingRepository.save(toneSetting);
        return ToneSettingResponse.from(saved);
    }

    // 4. Q&A 호출에 반영할 톤 문자열 구성. 선택된 프리셋 전부의 설명을 이어붙이고 customText를 덧붙인다.
    // 로그인하지 않았거나 저장된 설정이 없으면 beginner 기본값을 사용한다.
    public String resolveToneText(Long userId, String lang) {
        List<String> presetKeys = List.of(TonePresets.DEFAULT_PRESET_KEY);
        String customText = null;

        if (userId != null) {
            ToneSetting toneSetting = toneSettingRepository.findByUserId(userId)
                    .orElse(null);
            if (toneSetting != null) {
                presetKeys = toneSetting.getPresetKeys();
                customText = toneSetting.getCustomText();
            }
        }

        String combinedDescription = presetKeys.stream()
                .map(presetKey -> TonePresets.getDescription(presetKey, lang))
                .collect(Collectors.joining(" "));

        if (customText == null || customText.isBlank()) {
            return combinedDescription;
        }

        return combinedDescription + " " + customText.trim();
    }

    // 5. 사용자가 톤 커스텀 텍스트를 문자 체계가 다른 언어(한글/한자·가나/아랍/키릴 등)로 직접 적었으면,
    // 그게 요청 파라미터의 lang보다 더 신뢰할 수 있는 "실제 원하는 답변 언어" 신호다. LLM에게 판별을
    // 맡기는 방식은 신뢰할 수 없어서(라이브 검증됨) 유니코드 블록으로 결정적으로 감지한다.
    public Optional<String> detectLanguageOverride(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return toneSettingRepository.findByUserId(userId)
                .map(ToneSetting::getCustomText)
                .flatMap(LanguageScriptDetector::detect);
    }

    private String resolveRequesterNativeLang(Long requesterId) {
        if (requesterId == null) {
            throw new InvalidCredentialsException("로그인이 필요합니다");
        }

        AppUser user = appUserRepository.findById(requesterId)
                .orElseThrow(() -> new InvalidCredentialsException("로그인이 필요합니다"));

        return user.getNativeLang();
    }
}
