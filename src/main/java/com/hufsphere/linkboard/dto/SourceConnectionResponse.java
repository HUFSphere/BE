package com.hufsphere.linkboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceConnectionResponse {

    private Long id;
    private Long workspaceId;
    private String sourceType;
    private String status;
    private String targetRepoOrBoard;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
}