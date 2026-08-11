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
public class SourceSyncResponse {

    private Long connectionId;
    private String status;
    private String message;
    private LocalDateTime syncedAt;
}