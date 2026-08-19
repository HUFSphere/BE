package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.client.dto.GithubRepoItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "GitHub 레포지토리")
public class GithubRepoResponse {

    @Schema(description = "레포지토리 전체 이름(owner/repo). 소스 연동 생성(POST /source-connections)의 targetRepoOrBoard로 그대로 사용한다", example = "HUFSphere/BE")
    private final String fullName;

    @Schema(description = "비공개 레포지토리 여부", example = "false")
    private final boolean privateRepo;

    @Schema(description = "기본 브랜치", example = "main")
    private final String defaultBranch;

    public static GithubRepoResponse from(GithubRepoItem item) {
        return new GithubRepoResponse(item.getFullName(), item.isPrivateRepo(), item.getDefaultBranch());
    }
}
