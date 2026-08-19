package com.hufsphere.linkboard.client.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AskResponse {

    private String answer;
    private List<AskSourceItem> sources;
    private List<String> followUpQuestions;
    private List<TeamNormMatchDto> relatedTeamNorms;
}
