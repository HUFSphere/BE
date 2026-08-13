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
public class WorkspaceSettingResponse {

    private Long id;
    private String name;
    private String description;
    private String defaultLanguage;
    private LocalDateTime updatedAt;
}