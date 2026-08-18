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

    // 더 이상 쓰이지 않음: 자격 증명은 /api/v1/auth/{source}/authorize -> /callback OAuth로 저장된다.
    private String accessToken;
}