package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.domain.TeamNorm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "답변과 관련된 팀 관행")
public class TeamNormCardResponse {

    @Schema(description = "팀 관행 ID", example = "3")
    private final Long id;

    @Schema(description = "분류", example = "CODE_REVIEW")
    private final String category;

    @Schema(description = "팀 관행 내용", example = "PR은 항상 리뷰어 2명 이상 승인 후 머지한다")
    private final String content;

    @Schema(description = "이 답변과 관련 있는 이유", example = "JWT 도입도 PR 리뷰 절차를 거쳐 머지되었습니다")
    private final String reason;

    public static TeamNormCardResponse of(TeamNorm norm, String reason) {
        return new TeamNormCardResponse(norm.getId(), norm.getCategory(), norm.getContent(), reason);
    }
}
