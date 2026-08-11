package com.hufsphere.linkboard.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GithubIngestResponse {

    private String repo;
    private int indexed;
}
