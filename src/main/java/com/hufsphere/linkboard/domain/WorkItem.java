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

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private String itemType;
    private Long sourceNumber;
    private String title;
    private String status;
    private String summaryNative;
    private String authorLogin;
    private String sourceUrl;
    private LocalDateTime sourceUpdatedAt;
}