package com.hufsphere.linkboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// GitHub/Notion/Figma 세 플랫폼의 동기화 내용을 AI가 분석해서 자동으로 채워지는 팀 관행.
// 사용자가 직접 입력/수정하지 않고, 소스 동기화마다 워크스페이스 단위로 통째로 재생성된다
// (TeamNormSyncService 참고). 수동 CRUD는 폐지됨.
@Entity
@Table(name = "team_norms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TeamNorm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(nullable = false, length = 100)
    private String category; // 예: COMMUNICATION, CODE_REVIEW, MEETING, CONVENTION 등 (AI가 판정)

    // 실제 기록에서 반복 관찰된 패턴을 요약한 1문장. 예: "최근 PR 12건 중 10건이 리뷰어 2명의
    // 승인을 받은 뒤 머지되었습니다."
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 이 관행의 근거가 되는 기록 1개(가장 대표적인 것 하나만). replaceForWorkspace로 생성되는
    // 값이라 항상 채워지지만, 기존에 수동으로 입력된 row가 남아있는 테이블에 NOT NULL 컬럼을
    // 추가하면 (ddl-auto: update) 마이그레이션이 실패할 수 있어 DB 제약은 nullable로 둔다.
    @Column(name = "evidence_url", length = 1000)
    private String evidenceUrl;

    @Column(name = "evidence_title", length = 500)
    private String evidenceTitle;

    @Column(name = "evidence_source_type", length = 50)
    private String evidenceSourceType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
