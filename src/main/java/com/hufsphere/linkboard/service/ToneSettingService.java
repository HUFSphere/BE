package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.common.TonePresets;
import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.domain.ToneSetting;
import com.hufsphere.linkboard.dto.request.ToneSettingRequest;
import com.hufsphere.linkboard.dto.response.ToneSettingResponse;
import com.hufsphere.linkboard.dto.response.TonePresetResponse;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.exception.WorkspaceAccessDeniedException;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.repository.ToneSettingRepository;
import com.hufsphere.linkboard.repository.WorkspaceMemberRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ToneSettingService {

    private final ToneSettingRepository toneSettingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AppUserRepository appUserRepository;

    // 1. 톤 프리셋 목록 조회. lang이 없으면 요청자의 native_lang을 사용한다.
    public List<TonePresetResponse> getPresets(Long requesterId, String lang) {
        String resolvedLang = (lang != null && !lang.isBlank()) ? lang : resolveRequesterNativeLang(requesterId);

        return TonePresets.getPresets(resolvedLang).stream()
                .map(TonePresetResponse::from)
                .toList();
    }

    // 3. 톤 설정 조회. 저장된 설정이 없으면 기본값(beginner)을 에러 없이 반환한다.
    public ToneSettingResponse getToneSetting(Long workspaceId, Long userId) {
        validateMembership(workspaceId, userId);

        return toneSettingRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(ToneSettingResponse::from)
                .orElseGet(ToneSettingResponse::defaultResponse);
    }

    // 2. 톤 설정 저장(upsert). beginner/intermediate/expert 중 하나 이상 중복 선택 가능
    @Transactional
    public ToneSettingResponse saveToneSetting(Long workspaceId, Long userId, ToneSettingRequest request) {
        validateMembership(workspaceId, userId);

        List<String> presetKeys = request.getPresetKeys().stream().distinct().toList();
        for (String presetKey : presetKeys) {
            if (!TonePresets.isValidPresetKey(presetKey)) {
                throw new IllegalArgumentException("presetKeys는 beginner/intermediate/expert로만 구성되어야 합니다");
            }
        }

        ToneSetting toneSetting = toneSettingRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseGet(() -> ToneSetting.builder()
                        .workspaceId(workspaceId)
                        .userId(userId)
                        .build());

        toneSetting.update(presetKeys, request.getCustomText());

        ToneSetting saved = toneSettingRepository.save(toneSetting);
        return ToneSettingResponse.from(saved);
    }

    // 4. Q&A 호출에 반영할 톤 문자열 구성. 선택된 프리셋 전부의 설명을 이어붙이고 customText를 덧붙인다.
    // 로그인하지 않았거나 저장된 설정이 없으면 beginner 기본값을 사용한다.
    public String resolveToneText(Long workspaceId, Long userId, String lang) {
        List<String> presetKeys = List.of(TonePresets.DEFAULT_PRESET_KEY);
        String customText = null;

        if (userId != null) {
            ToneSetting toneSetting = toneSettingRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
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

    private String resolveRequesterNativeLang(Long requesterId) {
        if (requesterId == null) {
            throw new InvalidCredentialsException("로그인이 필요합니다");
        }

        AppUser user = appUserRepository.findById(requesterId)
                .orElseThrow(() -> new InvalidCredentialsException("로그인이 필요합니다"));

        return user.getNativeLang();
    }

    private void validateMembership(Long workspaceId, Long userId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다");
        }

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new WorkspaceAccessDeniedException("이 워크스페이스에 접근 권한이 없습니다");
        }
    }
}
