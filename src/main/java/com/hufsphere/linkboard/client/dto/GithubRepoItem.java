package com.hufsphere.linkboard.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepoItem {

    private Long id;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("private")
    private boolean privateRepo;

    @JsonProperty("default_branch")
    private String defaultBranch;
}
