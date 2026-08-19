package com.hufsphere.linkboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.dto.AskResponse;
import com.hufsphere.linkboard.domain.ToneSetting;
import com.hufsphere.linkboard.dto.request.QnaRequest;
import com.hufsphere.linkboard.repository.AppUserRepository;
import com.hufsphere.linkboard.repository.ToneSettingRepository;
import com.hufsphere.linkboard.repository.WorkspaceMemberRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QnaServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private AiServerClient aiServerClient;
    @Mock
    private ToneSettingRepository toneSettingRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private AppUserRepository appUserRepository;

    private QnaService qnaService;

    @BeforeEach
    void setUp() {
        ToneSettingService toneSettingService = new ToneSettingService(
                toneSettingRepository, workspaceRepository, workspaceMemberRepository, appUserRepository
        );
        qnaService = new QnaService(workspaceRepository, aiServerClient, toneSettingService);
    }

    private static ToneSetting toneSettingOf(String presetKey) {
        return ToneSetting.builder()
                .workspaceId(1L)
                .userId(1L)
                .presetKey(presetKey)
                .build();
    }

    private static QnaRequest requestOf(String question, String lang) {
        QnaRequest request = new QnaRequest();
        ReflectionTestUtils.setField(request, "question", question);
        ReflectionTestUtils.setField(request, "lang", lang);
        return request;
    }

    private static AskResponse askResponseOf(String answer) {
        AskResponse response = new AskResponse();
        response.setAnswer(answer);
        response.setSources(List.of());
        return response;
    }

    @Test
    void beginner_톤과_expert_톤은_서로_다른_상세도의_지시문이_AI_서버로_전달된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(aiServerClient.ask(any(), any(), any())).thenReturn(askResponseOf("답변"));

        QnaRequest request = requestOf("왜 JWT를 썼어요?", "ko");

        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(toneSettingOf("beginner")));
        qnaService.ask(1L, 10L, request);

        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 20L))
                .thenReturn(Optional.of(toneSettingOf("expert")));
        qnaService.ask(1L, 20L, request);

        ArgumentCaptor<String> toneCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiServerClient, times(2)).ask(eq("왜 JWT를 썼어요?"), eq("ko"), toneCaptor.capture());

        String beginnerTone = toneCaptor.getAllValues().get(0);
        String expertTone = toneCaptor.getAllValues().get(1);

        assertThat(beginnerTone).isNotEqualTo(expertTone);
        assertThat(beginnerTone).contains("자세히 설명해주세요");
        assertThat(expertTone).contains("간결하게");
        assertThat(beginnerTone.length()).isGreaterThan(expertTone.length());
    }

    @Test
    void 저장된_톤_설정이_없으면_기본값_beginner_톤이_전달된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 99L)).thenReturn(Optional.empty());
        when(aiServerClient.ask(any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, 99L, requestOf("질문", "ko"));

        verify(aiServerClient).ask(eq("질문"), eq("ko"), eq(
                "저는 새로 합류한 주니어입니다. 기술 결정의 이유와 배경을 자세히 설명해주세요. 팀의 관행이나 암묵적 규칙도 함께 알려주시면 좋겠습니다."
        ));
    }

    @Test
    void 로그인하지_않은_요청도_기본_톤으로_처리된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(aiServerClient.ask(any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, null, requestOf("질문", "en"));

        verify(aiServerClient).ask(eq("질문"), eq("en"), eq(
                "I'm a junior developer who just joined. Please explain technical decisions in detail with context and background. Include any team conventions or implicit rules."
        ));
    }

    @Test
    void 커스텀_텍스트는_프리셋_설명_뒤에_덧붙여진다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(ToneSetting.builder()
                        .workspaceId(1L)
                        .userId(10L)
                        .presetKey("beginner")
                        .customText("특히 Spring 관련 결정은 더 자세히 설명해주세요")
                        .build()));
        when(aiServerClient.ask(any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, 10L, requestOf("질문", "ko"));

        verify(aiServerClient).ask(eq("질문"), eq("ko"), eq(
                "저는 새로 합류한 주니어입니다. 기술 결정의 이유와 배경을 자세히 설명해주세요. "
                        + "팀의 관행이나 암묵적 규칙도 함께 알려주시면 좋겠습니다. 특히 Spring 관련 결정은 더 자세히 설명해주세요"
        ));
    }
}
