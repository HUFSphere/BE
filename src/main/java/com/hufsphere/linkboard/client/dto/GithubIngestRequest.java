package com.hufsphere.linkboard.client.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GithubIngestRequest {

    private final String repo;
    private final int months;
}
