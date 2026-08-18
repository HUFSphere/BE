package com.hufsphere.linkboard.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SourceConnectionCreateRequest {

    private String sourceType;
    private String targetRepoOrBoard;
    private String accessToken;
}