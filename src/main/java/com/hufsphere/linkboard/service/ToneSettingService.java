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
public class ToneSettingService {

    private final ToneSettingRepository toneSettingRepository;
    private final AppUserRepository appUserRepository;
    private final TonePresetTranslationService tonePresetTranslationService;

    // 1. 톤 프리셋 목록 조회. lang이 없으면 요청자의 native_lang을 사용한다. ko/en이 아닌 언어는
    // TonePresetTranslationService가 AI로 실시간 번역해서(캐싱됨) 반환한다.
    // 캐시 미스 시 내부적으로 쓰기(saveAll)가 일어날 수 있어서 이 메서드는 readOnly 트랜잭션으로
    // 감싸면 안 된다(감싸면 "Connection is read-only" 에러로 캐시 저장이 실패한다).
    public List<TonePresetResponse> getPresets(Long requesterId, String lang) {
        String resolvedLang = (lang != null && !lang.isBlank()) ? lang : resolveRequesterNativeLang(requesterId);

        return tonePresetTranslationService.resolvePresets(resolvedLang).stream()
                .map(TonePresetResponse::from)
                .toList();
    }

    // 3. 톤 설정 조회. 저장된 설정이 없으면 기본값("선택 안 함" = 빈 presetKeys, customText null)을
    // 에러 없이 반환한다. 워크스페이스 단위가 아니라 사용자 개인 단위 설정이라, 로그인만 되어 있으면
    // (컨트롤러에서 검증) 워크스페이스 소속 여부와 무관하게 조회/저장할 수 있다.
    @Transactional(readOnly = true)
    public ToneSettingResponse getToneSetting(Long userId) {
        return toneSettingRepository.findByUserId(userId)
                .map(ToneSettingResponse::from)
                .orElseGet(ToneSettingResponse::defaultResponse);
    }

    // 2. 톤 설정 저장(upsert). concise/detailed/friendly 중 0개 이상 중복 선택 가능(프리셋 없이
    // customText만 저장하는 것도 허용).
    @Transactional
    public ToneSettingResponse saveToneSetting(Long userId, ToneSettingRequest request) {
        List<String> presetKeys = request.getPresetKeys() != null
                ? request.getPresetKeys().stream().distinct().toList()
                : List.of();
        for (String presetKey : presetKeys) {
            if (!TonePresets.isValidPresetKey(presetKey)) {
                throw new IllegalArgumentException("presetKeys는 concise/detailed/friendly로만 구성되어야 합니다");
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
    // 로그인하지 않았거나 저장된 설정이 없거나 "선택 안 함" 상태면 빈 문자열을 반환해서(프리셋 미적용)
    // AI가 기존 기본 동작 그대로 답하게 한다.
    // getPresets와 같은 이유로 readOnly 트랜잭션으로 감싸지 않는다(캐시 미스 시 쓰기 발생 가능).
    public String resolveToneText(Long userId, String lang) {
        List<String> presetKeys = List.of();
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
                .map(presetKey -> tonePresetTranslationService.resolveDescription(presetKey, lang))
                .collect(Collectors.joining(" "));

        if (customText == null || customText.isBlank()) {
            return combinedDescription;
        }
        if (combinedDescription.isBlank()) {
            return customText.trim();
        }

        return combinedDescription + " " + customText.trim();
    }

    // 5. 사용자가 톤 커스텀 텍스트를 문자 체계가 다른 언어(한글/한자·가나/아랍/키릴 등)로 직접 적었으면,
    // 그게 요청 파라미터의 lang보다 더 신뢰할 수 있는 "실제 원하는 답변 언어" 신호다. LLM에게 판별을
    // 맡기는 방식은 신뢰할 수 없어서(라이브 검증됨) 유니코드 블록으로 결정적으로 감지한다.
    @Transactional(readOnly = true)
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
