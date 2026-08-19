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
import com.hufsphere.linkboard.repository.TeamNormRepository;
import com.hufsphere.linkboard.repository.ToneSettingRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
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
    private WorkItemRepository workItemRepository;
    @Mock
    private TeamNormRepository teamNormRepository;
    @Mock
    private AiServerClient aiServerClient;
    @Mock
    private ToneSettingRepository toneSettingRepository;
    @Mock
    private AppUserRepository appUserRepository;

    private QnaService qnaService;

    @BeforeEach
    void setUp() {
        ToneSettingService toneSettingService = new ToneSettingService(toneSettingRepository, appUserRepository);
        qnaService = new QnaService(workspaceRepository, workItemRepository, teamNormRepository, aiServerClient, toneSettingService);
    }

    private static ToneSetting toneSettingOf(List<String> presetKeys) {
        return ToneSetting.builder()
                .userId(1L)
                .presetKeys(presetKeys)
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
        response.setFollowUpQuestions(List.of());
        response.setRelatedTeamNorms(List.of());
        return response;
    }

    @Test
    void detailed_톤과_concise_톤은_서로_다른_지시문이_AI_서버로_전달된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        QnaRequest request = requestOf("왜 JWT를 썼어요?", "ko");

        when(toneSettingRepository.findByUserId(10L))
                .thenReturn(Optional.of(toneSettingOf(List.of("detailed"))));
        qnaService.ask(1L, 10L, request);

        when(toneSettingRepository.findByUserId(20L))
                .thenReturn(Optional.of(toneSettingOf(List.of("concise"))));
        qnaService.ask(1L, 20L, request);

        ArgumentCaptor<String> toneCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiServerClient, times(2)).ask(eq("왜 JWT를 썼어요?"), eq("ko"), toneCaptor.capture(), eq(List.of()));

        String detailedTone = toneCaptor.getAllValues().get(0);
        String conciseTone = toneCaptor.getAllValues().get(1);

        assertThat(detailedTone).isNotEqualTo(conciseTone);
        assertThat(detailedTone).contains("자세히 설명해주세요");
        assertThat(conciseTone).contains("간결하게");
    }

    @Test
    void 저장된_톤_설정이_없으면_프리셋_미적용_빈_톤이_전달된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(toneSettingRepository.findByUserId(99L)).thenReturn(Optional.empty());
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, 99L, requestOf("질문", "ko"));

        verify(aiServerClient).ask(eq("질문"), eq("ko"), eq(""), eq(List.of()));
    }

    @Test
    void 로그인하지_않은_요청도_빈_톤으로_처리된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, null, requestOf("질문", "en"));

        verify(aiServerClient).ask(eq("질문"), eq("en"), eq(""), eq(List.of()));
    }

    @Test
    void 커스텀_텍스트는_프리셋_설명_뒤에_덧붙여진다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(toneSettingRepository.findByUserId(10L))
                .thenReturn(Optional.of(ToneSetting.builder()
                        .userId(10L)
                        .presetKeys(List.of("detailed"))
                        .customText("특히 Spring 관련 결정은 더 자세히 설명해주세요")
                        .build()));
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, 10L, requestOf("질문", "ko"));

        verify(aiServerClient).ask(eq("질문"), eq("ko"), eq(
                "배경과 이유를 자세히 설명해주세요. 특히 Spring 관련 결정은 더 자세히 설명해주세요"
        ), eq(List.of()));
    }

    @Test
    void 프리셋_없이_customText만으로도_톤이_적용된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(toneSettingRepository.findByUserId(10L))
                .thenReturn(Optional.of(ToneSetting.builder()
                        .userId(10L)
                        .presetKeys(List.of())
                        .customText("이모지 많이 써주세요")
                        .build()));
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, 10L, requestOf("질문", "ko"));

        verify(aiServerClient).ask(eq("질문"), eq("ko"), eq("이모지 많이 써주세요"), eq(List.of()));
    }

    @Test
    void 프리셋을_여러_개_선택하면_각_프리셋_설명이_모두_톤에_포함된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(toneSettingRepository.findByUserId(10L))
                .thenReturn(Optional.of(toneSettingOf(List.of("detailed", "friendly"))));
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, 10L, requestOf("질문", "ko"));

        verify(aiServerClient).ask(eq("질문"), eq("ko"), eq(
                "배경과 이유를 자세히 설명해주세요. 친근하고 편안한 말투로 답변해주세요."
        ), eq(List.of()));
    }

    @Test
    void contextWorkItemIds가_있으면_해당_work_item으로_범위를_좁혀_qna를_호출한다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workItemRepository.findByIdInAndWorkspaceId(List.of(142L, 156L), 1L)).thenReturn(List.of());
        when(aiServerClient.qna(any(), any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        QnaRequest request = requestOf("이 기능들은 왜 이렇게 만들었어요?", "ko");
        ReflectionTestUtils.setField(request, "contextWorkItemIds", List.of(142L, 156L));

        qnaService.ask(1L, null, request);

        verify(aiServerClient).qna(eq("이 기능들은 왜 이렇게 만들었어요?"), eq("ko"), any(), eq(List.of()), eq(List.of()));
        verify(aiServerClient, times(0)).ask(any(), any(), any(), any());
    }

    @Test
    void 톤_customText가_아랍어면_요청_lang_대신_아랍어로_AI_서버를_호출한다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(toneSettingRepository.findByUserId(10L))
                .thenReturn(Optional.of(ToneSetting.builder()
                        .userId(10L)
                        .presetKeys(List.of("detailed"))
                        .customText("أرجو أن تجيب باللغة العربية فقط من فضلك")
                        .build()));
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, 10L, requestOf("질문", "ko"));

        verify(aiServerClient).ask(eq("질문"), eq("ar"), any(), eq(List.of()));
    }

    @Test
    void contextWorkItemIds가_없으면_기존처럼_전역_검색_ask를_호출한다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, null, requestOf("질문", "ko"));

        verify(aiServerClient).ask(any(), any(), any(), any());
        verify(aiServerClient, times(0)).qna(any(), any(), any(), any(), any());
    }
}
