package com.hufsphere.linkboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.dto.TonePresetItemDto;
import com.hufsphere.linkboard.client.dto.TranslateTonePresetsResponse;
import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.domain.ToneSetting;
import com.hufsphere.linkboard.dto.request.ToneSettingRequest;
import com.hufsphere.linkboard.dto.response.ToneSettingResponse;
import com.hufsphere.linkboard.dto.response.TonePresetResponse;
import com.hufsphere.linkboard.exception.InvalidCredentialsException;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.repository.ToneSettingRepository;
import com.hufsphere.linkboard.repository.TonePresetTranslationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ToneSettingServiceTest {

    @Mock
    private ToneSettingRepository toneSettingRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private TonePresetTranslationRepository tonePresetTranslationRepository;
    @Mock
    private AiServerClient aiServerClient;

    private ToneSettingService toneSettingService;

    @BeforeEach
    void setUp() {
        // ko/en은 TonePresetTranslationService가 하드코딩된 TonePresets를 그대로 반환하는 경로만
        // 타므로(레포지토리/AI 클라이언트 미호출), 실제 인스턴스를 그대로 써도 ko/en 테스트는
        // 네트워크 호출 없이 결정적으로 동작한다.
        TonePresetTranslationService tonePresetTranslationService =
                new TonePresetTranslationService(tonePresetTranslationRepository, aiServerClient);
        toneSettingService = new ToneSettingService(toneSettingRepository, appUserRepository, tonePresetTranslationService);
    }

    private static ToneSettingRequest requestOf(List<String> presetKeys, String customText) {
        ToneSettingRequest request = new ToneSettingRequest();
        ReflectionTestUtils.setField(request, "presetKeys", presetKeys);
        ReflectionTestUtils.setField(request, "customText", customText);
        return request;
    }

    @Test
    void 프리셋_목록은_언어별로_다르게_반환된다() {
        List<TonePresetResponse> ko = toneSettingService.getPresets(null, "ko");
        List<TonePresetResponse> en = toneSettingService.getPresets(null, "en");

        assertThat(ko).extracting(TonePresetResponse::getPresetKey)
                .containsExactly("concise", "detailed", "friendly");
        assertThat(ko.get(0).getLabel()).isEqualTo("간결하게");
        assertThat(en.get(0).getLabel()).isEqualTo("Concise");
    }

    // ko/en이 아닌 언어는 하드코딩된 대체(예전엔 무조건 영어로 대체)가 아니라, AI 서버로 실시간
    // 번역을 시도해야 한다. 번역 내용 자체(문구)는 AI가 결정하므로 검증하지 않고, 그 경로를
    // 실제로 타는지(캐시 미스 -> AI 호출 -> 저장)만 확인한다.
    @Test
    void 지원하지_않는_언어는_AI로_번역을_시도하고_결과를_캐시에_저장한다() {
        when(tonePresetTranslationRepository.findByLang("de")).thenReturn(List.of());

        TranslateTonePresetsResponse response = new TranslateTonePresetsResponse();
        response.setPresets(List.of(
                new TonePresetItemDto("concise", "Kurz", "Bitte antworten Sie kurz."),
                new TonePresetItemDto("detailed", "Detailliert", "Bitte erklären Sie ausführlich."),
                new TonePresetItemDto("friendly", "Freundlich", "Bitte antworten Sie freundlich.")
        ));
        when(aiServerClient.translateTonePresets(eq("de"), any())).thenReturn(response);
        when(tonePresetTranslationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TonePresetResponse> other = toneSettingService.getPresets(null, "de");

        assertThat(other).extracting(TonePresetResponse::getLabel)
                .containsExactly("Kurz", "Detailliert", "Freundlich");
        verify(aiServerClient).translateTonePresets(eq("de"), any());
    }

    @Test
    void lang이_없으면_요청자의_모국어_프리셋을_반환한다() {
        AppUser user = AppUser.builder()
                .email("a@hufs.ac.kr")
                .name("박재영")
                .nativeLang("en")
                .build();
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        List<TonePresetResponse> presets = toneSettingService.getPresets(1L, null);

        assertThat(presets.get(0).getLabel()).isEqualTo("Concise");
    }

    @Test
    void lang도_없고_로그인도_안했으면_예외를_던진다() {
        assertThatThrownBy(() -> toneSettingService.getPresets(null, null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void 톤_설정이_없으면_선택_안_함_기본값을_에러없이_반환한다() {
        when(toneSettingRepository.findByUserId(10L)).thenReturn(Optional.empty());

        ToneSettingResponse response = toneSettingService.getToneSetting(10L);

        assertThat(response.getPresetKeys()).isEmpty();
        assertThat(response.getCustomText()).isNull();
        assertThat(response.getUpdatedAt()).isNull();
    }

    @Test
    void 톤_설정은_워크스페이스와_무관하게_사용자_단위로_조회된다() {
        ToneSetting existing = ToneSetting.builder()
                .userId(10L)
                .presetKeys(List.of("friendly"))
                .build();
        when(toneSettingRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

        ToneSettingResponse response = toneSettingService.getToneSetting(10L);

        assertThat(response.getPresetKeys()).containsExactly("friendly");
    }

    @Test
    void 프리셋을_여러_개_중복_선택해_저장할_수_있다() {
        when(toneSettingRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(toneSettingRepository.save(any(ToneSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ToneSettingResponse response = toneSettingService.saveToneSetting(
                10L, requestOf(List.of("concise", "friendly"), "Spring 결정은 더 자세히 설명해주세요"));

        assertThat(response.getPresetKeys()).containsExactly("concise", "friendly");
        assertThat(response.getCustomText()).isEqualTo("Spring 결정은 더 자세히 설명해주세요");
    }

    @Test
    void 프리셋_없이_customText만으로도_저장할_수_있다() {
        when(toneSettingRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(toneSettingRepository.save(any(ToneSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ToneSettingResponse response = toneSettingService.saveToneSetting(
                10L, requestOf(List.of(), "이모지 많이 써주세요"));

        assertThat(response.getPresetKeys()).isEmpty();
        assertThat(response.getCustomText()).isEqualTo("이모지 많이 써주세요");
    }

    @Test
    void presetKeys가_null이어도_저장된다() {
        when(toneSettingRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(toneSettingRepository.save(any(ToneSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ToneSettingResponse response = toneSettingService.saveToneSetting(10L, requestOf(null, "짧게"));

        assertThat(response.getPresetKeys()).isEmpty();
        assertThat(response.getCustomText()).isEqualTo("짧게");
    }

    @Test
    void 중복된_presetKey는_저장시_하나로_합쳐진다() {
        when(toneSettingRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(toneSettingRepository.save(any(ToneSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ToneSettingResponse response = toneSettingService.saveToneSetting(
                10L, requestOf(List.of("concise", "concise"), null));

        assertThat(response.getPresetKeys()).containsExactly("concise");
    }

    @Test
    void 이미_설정이_있으면_저장시_갱신된다() {
        ToneSetting existing = ToneSetting.builder()
                .userId(10L)
                .presetKeys(List.of("concise"))
                .build();
        when(toneSettingRepository.findByUserId(10L)).thenReturn(Optional.of(existing));
        when(toneSettingRepository.save(any(ToneSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ToneSettingResponse response =
                toneSettingService.saveToneSetting(10L, requestOf(List.of("detailed"), null));

        assertThat(response.getPresetKeys()).containsExactly("detailed");
        assertThat(response.getCustomText()).isNull();
    }

    @Test
    void presetKeys에_유효하지_않은_값이_있으면_저장시_예외를_던진다() {
        assertThatThrownBy(() -> toneSettingService.saveToneSetting(10L, requestOf(List.of("guru"), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customText가_다른_문자체계_언어면_언어_오버라이드를_감지한다() {
        ToneSetting existing = ToneSetting.builder()
                .userId(10L)
                .presetKeys(List.of("concise"))
                .customText("أرجو أن تجيب باللغة العربية فقط من فضلك")
                .build();
        when(toneSettingRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

        assertThat(toneSettingService.detectLanguageOverride(10L)).contains("ar");
    }

    @Test
    void customText가_없거나_로마자_계열이면_언어_오버라이드가_없다() {
        when(toneSettingRepository.findByUserId(10L)).thenReturn(Optional.empty());
        assertThat(toneSettingService.detectLanguageOverride(10L)).isEmpty();

        ToneSetting englishCustom = ToneSetting.builder()
                .userId(20L)
                .presetKeys(List.of("concise"))
                .customText("Please keep it short")
                .build();
        when(toneSettingRepository.findByUserId(20L)).thenReturn(Optional.of(englishCustom));
        assertThat(toneSettingService.detectLanguageOverride(20L)).isEmpty();
    }

    @Test
    void 로그인하지_않았으면_언어_오버라이드를_감지하지_않는다() {
        assertThat(toneSettingService.detectLanguageOverride(null)).isEmpty();
    }
}
