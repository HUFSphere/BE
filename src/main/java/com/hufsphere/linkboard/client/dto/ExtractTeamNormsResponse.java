package com.hufsphere.linkboard.client.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExtractTeamNormsResponse {

    private List<TeamNormCandidateDto> teamNorms;
}
