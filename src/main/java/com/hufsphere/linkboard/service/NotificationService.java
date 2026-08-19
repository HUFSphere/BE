package com.hufsphere.linkboard.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public NotificationListResponse getNotifications(Long userId, int limit) {
        List<NotificationResponse> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
                .limit(limit)
                .map(NotificationResponse::from)
                .toList();

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        return NotificationListResponse.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .build();
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "알림을 찾을 수 없습니다. notificationId=" + notificationId));

        notification.markAsRead();

        return NotificationResponse.from(notification);
    }

    @Transactional
    public void notifySourceSyncResult(Workspace workspace, SourceType sourceType, boolean success) {
        String message = success
                ? sourceType.getValue() + " 동기화가 완료되었습니다"
                : sourceType.getValue() + " 동기화에 실패했습니다";
        NotificationType type = success ? NotificationType.SOURCE_SYNC_SUCCESS : NotificationType.SOURCE_SYNC_FAILURE;

        fanOutToWorkspace(workspace, type, message);
    }

    @Transactional
    public void notifyMemberInviteAccepted(Workspace workspace, AppUser newMember) {
        AppUser owner = workspace.getOwner();
        if (owner == null || owner.getId().equals(newMember.getId())) {
            return;
        }

        save(owner.getId(), workspace.getId(), NotificationType.MEMBER_INVITE_ACCEPTED,
                newMember.getName() + "님이 워크스페이스에 참여했습니다");
    }

    @Transactional
    public void notifyNewWorkItemsDetected(Workspace workspace, int newItemCount) {
        if (newItemCount <= 0) {
            return;
        }

        fanOutToWorkspace(workspace, NotificationType.NEW_WORK_ITEMS_DETECTED,
                "새 작업 항목 " + newItemCount + "개가 감지되었습니다");
    }

    private void fanOutToWorkspace(Workspace workspace, NotificationType type, String message) {
        List<WorkspaceMember> members = workspaceMemberRepository.findAllByWorkspaceId(workspace.getId());
        for (WorkspaceMember member : members) {
            save(member.getUser().getId(), workspace.getId(), type, message);
        }
    }

    private void save(Long userId, Long workspaceId, NotificationType type, String message) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .workspaceId(workspaceId)
                .type(type)
                .message(message)
                .build());
    }
}
