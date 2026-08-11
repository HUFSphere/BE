package com.hufsphere.linkboard.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "work_item_link")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkItemLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_work_item_id", nullable = false)
    private WorkItem fromWorkItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_work_item_id", nullable = false)
    private WorkItem toWorkItem;

    private String linkSource;
}