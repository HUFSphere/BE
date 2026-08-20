package com.hufsphere.linkboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.dto.TonePresetItemDto;
import com.hufsphere.linkboard.client.dto.TranslateTonePresetsResponse;
import com.hufsphere.linkboard.common.TonePresets;
import com.hufsphere.linkboard.domain.TonePresetTranslation;
import com.hufsphere.linkboard.repository.TonePresetTranslationRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TonePresetTranslationServiceTest {

    @Mock
    private TonePresetTranslationRepository translationRepository;
    @Mock
    private AiServerClient aiServerClient;

    @InjectMocks
    private TonePresetTranslationService service;

    @Test
    void ko와_en은_레포지토리와_AI서버를_전혀_호출하지_않는다() {
        List<TonePresets.TonePreset> ko = service.resolvePresets("ko");
        List<TonePresets.TonePreset> en = service.resolvePresets("en");

        assertThat(ko).extracting(TonePresets.TonePreset::presetKey)
                .containsExactly("concise", "detailed", "friendly");
        assertThat(en.get(0).label()).isEqualTo("Concise");
        verify(translationRepository, never()).findByLang(any());
        verify(aiServerClient, never()).translateTonePresets(any(), any());
    }

    @Test
    void 캐시에_있으면_AI서버를_호출하지_않고_캐시를_그대로_반환한다() {
        TonePresetTranslation cached = TonePresetTranslation.builder()
                .lang("ja").presetKey("concise").label("簡潔に").description("要点だけ簡潔に答えてください。")
                .build();
        when(translationRepository.findByLang("ja")).thenReturn(List.of(cached));

        List<TonePresets.TonePreset> result = service.resolvePresets("ja");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).label()).isEqualTo("簡潔に");
        verify(aiServerClient, never()).translateTonePresets(any(), any());
    }

    @Test
    void 캐시가_없으면_AI서버로_번역해서_저장한다() {
        when(translationRepository.findByLang("vi")).thenReturn(List.of());
        TranslateTonePresetsResponse response = new TranslateTonePresetsResponse();
        response.setPresets(List.of(
                new TonePresetItemDto("concise", "Ngắn gọn", "Vui lòng trả lời ngắn gọn."),
                new TonePresetItemDto("detailed", "Chi tiết", "Vui lòng giải thích chi tiết."),
                new TonePresetItemDto("friendly", "Thân thiện", "Vui lòng trả lời một cách thân thiện.")
        ));
        when(aiServerClient.translateTonePresets(eq("vi"), any())).thenReturn(response);
        when(translationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TonePresets.TonePreset> result = service.resolvePresets("vi");

        assertThat(result).extracting(TonePresets.TonePreset::label)
                .containsExactly("Ngắn gọn", "Chi tiết", "Thân thiện");
    }

    // 동시에 같은 언어를 처음 요청한 다른 스레드가 먼저 저장을 끝내면 saveAll이 유니크 제약
    // 위반으로 실패할 수 있다. 이때 예외를 그대로 던지지 않고, 이미 저장된 캐시를 다시 읽어와야 한다.
    @Test
    void 저장중_동시요청으로_유니크제약_위반이_나면_기존_캐시를_다시_읽어온다() {
        when(translationRepository.findByLang("fr"))
                .thenReturn(List.of())
                .thenReturn(List.of(TonePresetTranslation.builder()
                        .lang("fr").presetKey("concise").label("Concis").description("Répondez brièvement.")
                        .build()));
        TranslateTonePresetsResponse response = new TranslateTonePresetsResponse();
        response.setPresets(List.of(new TonePresetItemDto("concise", "Concis", "Répondez brièvement.")));
        when(aiServerClient.translateTonePresets(eq("fr"), any())).thenReturn(response);
        when(translationRepository.saveAll(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        List<TonePresets.TonePreset> result = service.resolvePresets("fr");

        assertThat(result).extracting(TonePresets.TonePreset::label).containsExactly("Concis");
    }

    @Test
    void resolveDescription도_ko_en은_하드코딩된_설명을_바로_반환한다() {
        String description = service.resolveDescription("concise", "ko");
        assertThat(description).isEqualTo("핵심만 간결하게 답변해주세요.");
        verify(aiServerClient, never()).translateTonePresets(any(), any());
    }

    @Test
    void resolveDescription은_지원하지_않는_언어면_번역된_설명을_반환한다() {
        when(translationRepository.findByLang("th")).thenReturn(List.of());
        TranslateTonePresetsResponse response = new TranslateTonePresetsResponse();
        response.setPresets(List.of(
                new TonePresetItemDto("concise", "กระชับ", "โปรดตอบอย่างกระชับ"),
                new TonePresetItemDto("detailed", "ละเอียด", "โปรดอธิบายอย่างละเอียด"),
                new TonePresetItemDto("friendly", "เป็นกันเอง", "โปรดตอบอย่างเป็นกันเอง")
        ));
        when(aiServerClient.translateTonePresets(eq("th"), any())).thenReturn(response);
        when(translationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String description = service.resolveDescription("friendly", "th");

        assertThat(description).isEqualTo("โปรดตอบอย่างเป็นกันเอง");
    }
}
