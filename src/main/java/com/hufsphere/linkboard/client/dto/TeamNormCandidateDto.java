package com.hufsphere.linkboard.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TeamNormCandidateDto {

    private String category;
    private String content;
    private String evidenceUrl;
    private String evidenceTitle;
    private String evidenceSourceType;
}
