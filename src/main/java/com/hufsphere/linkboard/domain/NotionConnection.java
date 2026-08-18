package com.hufsphere.linkboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notion_connection")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotionConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    // TODO: 토큰 암호화 적용 (현재는 평문 저장)
    @Column(name = "access_token", nullable = false, length = 500)
    private String accessToken;

    @Column(name = "notion_workspace_id", nullable = false, length = 100)
    private String notionWorkspaceId;

    @Column(name = "notion_workspace_name", length = 200)
    private String notionWorkspaceName;

    @Column(name = "bot_id", length = 100)
    private String botId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private NotionConnection(Long workspaceId, String accessToken, String notionWorkspaceId,
            String notionWorkspaceName, String botId) {
        this.workspaceId = workspaceId;
        this.accessToken = accessToken;
        this.notionWorkspaceId = notionWorkspaceId;
        this.notionWorkspaceName = notionWorkspaceName;
        this.botId = botId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
