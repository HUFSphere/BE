package com.hufsphere.linkboard.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceUpdateRequest {

    private String name;
    private String description;
    private String defaultLanguage;
}