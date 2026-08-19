package com.hufsphere.linkboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ToneSettingServiceTest {

    @Mock
    private ToneSettingRepository toneSettingRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private ToneSettingService toneSettingService;

    private static ToneSettingRequest requestOf(List<String> presetKeys, String customText) {
        ToneSettingRequest request = new ToneSettingRequest();
        ReflectionTestUtils.setField(request, "presetKeys", presetKeys);
        ReflectionTestUtils.setField(request, "customText", customText);
        return request;
    }

    @Test
    void 프리셋_목록은_언어별로_다르게_반환된다() {
        List<TonePresetResponse> ko = toneSettingService.getPresets(null, "ko");
        List<TonePresetResponse> vi = toneSettingService.getPresets(null, "vi");

        assertThat(ko).extracting(TonePresetResponse::getPresetKey)
                .containsExactly("beginner", "intermediate", "expert");
        assertThat(ko.get(0).getLabel()).isEqualTo("이 팀이 처음이에요");
        assertThat(vi.get(0).getLabel()).isEqualTo("Tôi mới vào team");
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

        assertThat(presets.get(0).getLabel()).isEqualTo("I'm new to this team");
    }

    @Test
    void lang도_없고_로그인도_안했으면_예외를_던진다() {
        assertThatThrownBy(() -> toneSettingService.getPresets(null, null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void 톤_설정이_없으면_기본값을_에러없이_반환한다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 10L)).thenReturn(true);
        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

        ToneSettingResponse response = toneSettingService.getToneSetting(1L, 10L);

        assertThat(response.getPresetKeys()).containsExactly("beginner");
        assertThat(response.getCustomText()).isNull();
        assertThat(response.getUpdatedAt()).isNull();
    }

    @Test
    void 워크스페이스_멤버가_아니면_톤_설정_조회시_예외를_던진다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> toneSettingService.getToneSetting(1L, 10L))
                .isInstanceOf(WorkspaceAccessDeniedException.class);
    }

    @Test
    void 워크스페이스가_없으면_톤_설정_조회시_예외를_던진다() {
        when(workspaceRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> toneSettingService.getToneSetting(1L, 10L))
                .isInstanceOf(WorkspaceNotFoundException.class);
    }

    @Test
    void 프리셋을_여러_개_중복_선택해_저장할_수_있다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 10L)).thenReturn(true);
        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.empty());
        when(toneSettingRepository.save(any(ToneSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ToneSettingResponse response = toneSettingService.saveToneSetting(
                1L, 10L, requestOf(List.of("beginner", "expert"), "Spring 결정은 더 자세히 설명해주세요"));

        assertThat(response.getPresetKeys()).containsExactly("beginner", "expert");
        assertThat(response.getCustomText()).isEqualTo("Spring 결정은 더 자세히 설명해주세요");
    }

    @Test
    void 중복된_presetKey는_저장시_하나로_합쳐진다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 10L)).thenReturn(true);
        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.empty());
        when(toneSettingRepository.save(any(ToneSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ToneSettingResponse response = toneSettingService.saveToneSetting(
                1L, 10L, requestOf(List.of("beginner", "beginner"), null));

        assertThat(response.getPresetKeys()).containsExactly("beginner");
    }

    @Test
    void 이미_설정이_있으면_저장시_갱신된다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 10L)).thenReturn(true);

        ToneSetting existing = ToneSetting.builder()
                .workspaceId(1L)
                .userId(10L)
                .presetKeys(List.of("beginner"))
                .build();
        when(toneSettingRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.of(existing));
        when(toneSettingRepository.save(any(ToneSetting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ToneSettingResponse response =
                toneSettingService.saveToneSetting(1L, 10L, requestOf(List.of("intermediate"), null));

        assertThat(response.getPresetKeys()).containsExactly("intermediate");
        assertThat(response.getCustomText()).isNull();
    }

    @Test
    void presetKeys에_유효하지_않은_값이_있으면_저장시_예외를_던진다() {
        when(workspaceRepository.existsById(1L)).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> toneSettingService.saveToneSetting(1L, 10L, requestOf(List.of("guru"), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
