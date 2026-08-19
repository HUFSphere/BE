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
    private WorkItemRepository workItemRepository;
    @Mock
    private TeamNormRepository teamNormRepository;
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
        qnaService = new QnaService(workspaceRepository, workItemRepository, teamNormRepository, aiServerClient, toneSettingService);
    }

    private static ToneSetting toneSettingOf(List<String> presetKeys) {
        return ToneSetting.builder()
                .workspaceId(1L)
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
    void beginner_톤과_expert_톤은_서로_다른_상세도의_지시문이_AI_서버로_전달된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        QnaRequest request = requestOf("왜 JWT를 썼어요?", "ko");

        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(toneSettingOf(List.of("beginner"))));
        qnaService.ask(1L, 10L, request);

        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 20L))
                .thenReturn(Optional.of(toneSettingOf(List.of("expert"))));
        qnaService.ask(1L, 20L, request);

        ArgumentCaptor<String> toneCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiServerClient, times(2)).ask(eq("왜 JWT를 썼어요?"), eq("ko"), toneCaptor.capture(), eq(List.of()));

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
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, 99L, requestOf("질문", "ko"));

        verify(aiServerClient).ask(eq("질문"), eq("ko"), eq(
                "저는 새로 합류한 주니어입니다. 기술 결정의 이유와 배경을 자세히 설명해주세요. 팀의 관행이나 암묵적 규칙도 함께 알려주시면 좋겠습니다."
        ), eq(List.of()));
    }

    @Test
    void 로그인하지_않은_요청도_기본_톤으로_처리된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, null, requestOf("질문", "en"));

        verify(aiServerClient).ask(eq("질문"), eq("en"), eq(
                "I'm a junior developer who just joined. Please explain technical decisions in detail with context and background. Include any team conventions or implicit rules."
        ), eq(List.of()));
    }

    @Test
    void 커스텀_텍스트는_프리셋_설명_뒤에_덧붙여진다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(ToneSetting.builder()
                        .workspaceId(1L)
                        .userId(10L)
                        .presetKeys(List.of("beginner"))
                        .customText("특히 Spring 관련 결정은 더 자세히 설명해주세요")
                        .build()));
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, 10L, requestOf("질문", "ko"));

        verify(aiServerClient).ask(eq("질문"), eq("ko"), eq(
                "저는 새로 합류한 주니어입니다. 기술 결정의 이유와 배경을 자세히 설명해주세요. "
                        + "팀의 관행이나 암묵적 규칙도 함께 알려주시면 좋겠습니다. 특히 Spring 관련 결정은 더 자세히 설명해주세요"
        ), eq(List.of()));
    }

    @Test
    void 프리셋을_여러_개_선택하면_각_프리셋_설명이_모두_톤에_포함된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(toneSettingOf(List.of("beginner", "expert"))));
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, 10L, requestOf("질문", "ko"));

        verify(aiServerClient).ask(eq("질문"), eq("ko"), eq(
                "저는 새로 합류한 주니어입니다. 기술 결정의 이유와 배경을 자세히 설명해주세요. "
                        + "팀의 관행이나 암묵적 규칙도 함께 알려주시면 좋겠습니다. "
                        + "경험 많은 개발자입니다. 이 팀만의 특수한 결정과 주의점만 간결하게 알려주세요."
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
    void contextWorkItemIds가_없으면_기존처럼_전역_검색_ask를_호출한다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(aiServerClient.ask(any(), any(), any(), any())).thenReturn(askResponseOf("답변"));

        qnaService.ask(1L, null, requestOf("질문", "ko"));

        verify(aiServerClient).ask(any(), any(), any(), any());
        verify(aiServerClient, times(0)).qna(any(), any(), any(), any(), any());
    }
}
