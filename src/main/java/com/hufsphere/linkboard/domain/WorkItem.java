package com.hufsphere.linkboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_connection_id", nullable = false)
    private SourceConnection sourceConnection;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private String itemType;
    private Long sourceNumber;
    private String title;
    private String status;

    // AI가 추출한 요약(summary_brief)을 저장한다. 프로젝트 지도/작업 상세 조회 API가
    // 이미 이 필드명으로 읽고 있어 컬럼명을 맞추기 위해 그대로 재사용한다.
    private String summaryNative;

    @Lob
    private String content;

    private String authorLogin;
    private String sourceUrl;
    private LocalDateTime sourceUpdatedAt;
    private LocalDateTime createdAt;

    // AI가 group-features로 분류한 기능(feature)의 id. 실제 연관관계(FK) 대신
    // 다른 소스 참조 컬럼들(FigmaConnection.workspaceId 등)과 동일하게 단순 Long으로 둔다.
    @Setter
    private Long featureId;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = "todo";
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}