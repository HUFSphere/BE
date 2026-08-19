package com.hufsphere.linkboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hufsphere.linkboard.domain.AppUser;
import com.hufsphere.linkboard.domain.Notification;
import com.hufsphere.linkboard.domain.NotificationType;
import com.hufsphere.linkboard.domain.SourceType;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.domain.WorkspaceMember;
import com.hufsphere.linkboard.dto.response.NotificationListResponse;
import com.hufsphere.linkboard.dto.response.NotificationResponse;
import com.hufsphere.linkboard.exception.NotificationNotFoundException;
import com.hufsphere.linkboard.repository.NotificationRepository;
import com.hufsphere.linkboard.repository.WorkspaceMemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private NotificationService notificationService;

    private static AppUser userOf(long id, String name) {
        AppUser user = AppUser.builder().name(name).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Workspace workspaceOf(long id, AppUser owner) {
        Workspace workspace = Workspace.builder().owner(owner).build();
        workspace.setId(id);
        return workspace;
    }

    private static WorkspaceMember memberOf(AppUser user) {
        return WorkspaceMember.builder().user(user).role("member").build();
    }

    @Test
    void 소스_동기화_성공시_워크스페이스_멤버_전원에게_알림이_생성된다() {
        AppUser leader = userOf(1L, "팀장");
        AppUser member = userOf(2L, "팀원");
        Workspace workspace = workspaceOf(10L, leader);
        when(workspaceMemberRepository.findAllByWorkspaceId(10L))
                .thenReturn(List.of(memberOf(leader), memberOf(member)));

        notificationService.notifySourceSyncResult(workspace, SourceType.GITHUB, true);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getUserId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(captor.getAllValues())
                .allSatisfy(n -> {
                    assertThat(n.getType()).isEqualTo(NotificationType.SOURCE_SYNC_SUCCESS);
                    assertThat(n.getMessage()).contains("완료");
                });
    }

    @Test
    void 소스_동기화_실패시_실패_알림이_생성된다() {
        AppUser leader = userOf(1L, "팀장");
        Workspace workspace = workspaceOf(10L, leader);
        when(workspaceMemberRepository.findAllByWorkspaceId(10L)).thenReturn(List.of(memberOf(leader)));

        notificationService.notifySourceSyncResult(workspace, SourceType.NOTION, false);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.SOURCE_SYNC_FAILURE);
        assertThat(captor.getValue().getMessage()).contains("실패");
    }

    @Test
    void 팀원_초대_수락시_리더에게만_알림이_생성된다() {
        AppUser leader = userOf(1L, "팀장");
        AppUser newMember = userOf(2L, "새멤버");
        Workspace workspace = workspaceOf(10L, leader);

        notificationService.notifyMemberInviteAccepted(workspace, newMember);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.MEMBER_INVITE_ACCEPTED);
        assertThat(captor.getValue().getMessage()).contains("새멤버");
    }

    @Test
    void 새_작업_항목_개수가_0이면_알림을_생성하지_않는다() {
        Workspace workspace = workspaceOf(10L, userOf(1L, "팀장"));

        notificationService.notifyNewWorkItemsDetected(workspace, 0);

        verify(notificationRepository, never()).save(any());
        verify(workspaceMemberRepository, never()).findAllByWorkspaceId(any());
    }

    @Test
    void 새_작업_항목이_감지되면_워크스페이스_멤버_전원에게_알림이_생성된다() {
        AppUser leader = userOf(1L, "팀장");
        Workspace workspace = workspaceOf(10L, leader);
        when(workspaceMemberRepository.findAllByWorkspaceId(10L)).thenReturn(List.of(memberOf(leader)));

        notificationService.notifyNewWorkItemsDetected(workspace, 3);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.NEW_WORK_ITEMS_DETECTED);
        assertThat(captor.getValue().getMessage()).contains("3");
    }

    @Test
    void 목록_조회는_최근순으로_limit개까지만_반환하고_안읽음_개수는_limit과_무관하다() {
        AtomicLong idSeq = new AtomicLong(1);
        List<Notification> all = List.of(
                notificationOf(idSeq, 10L, true),
                notificationOf(idSeq, 10L, false),
                notificationOf(idSeq, 10L, false));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(99L)).thenReturn(all);
        when(notificationRepository.countByUserIdAndIsReadFalse(99L)).thenReturn(2L);

        NotificationListResponse response = notificationService.getNotifications(99L, 2);

        assertThat(response.getNotifications()).hasSize(2);
        assertThat(response.getUnreadCount()).isEqualTo(2L);
    }

    @Test
    void 본인_알림을_읽음_처리하면_read가_true가_된다() {
        Notification notification = notificationOf(new AtomicLong(5), 10L, false);
        when(notificationRepository.findByIdAndUserId(5L, 10L)).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.markAsRead(10L, 5L);

        assertThat(response.isRead()).isTrue();
    }

    @Test
    void 본인_알림이_아니면_읽음_처리시_예외를_던진다() {
        when(notificationRepository.findByIdAndUserId(5L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 5L))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    private static Notification notificationOf(AtomicLong idSeq, long userId, boolean read) {
        Notification notification = Notification.builder()
                .userId(userId)
                .workspaceId(1L)
                .type(NotificationType.SOURCE_SYNC_SUCCESS)
                .message("github 동기화가 완료되었습니다")
                .build();
        ReflectionTestUtils.setField(notification, "id", idSeq.getAndIncrement());
        if (read) {
            notification.markAsRead();
        }
        return notification;
    }
}
