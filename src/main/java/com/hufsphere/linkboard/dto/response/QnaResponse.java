package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.client.dto.AskResponse;
import com.hufsphere.linkboard.client.dto.TeamNormMatchDto;
import com.hufsphere.linkboard.domain.TeamNorm;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "Q&A 응답")
public class QnaResponse {

    @Schema(description = "모국어 답변", example = "Vì lý do mở rộng stateless, nhóm đã chọn JWT thay cho session...")
    private final String answer;

    @Schema(description = "답변 근거 목록")
    private final List<QnaSourceResponse> sources;

    @Schema(description = "이어서 물어볼 만한 후속 질문 4개")
    private final List<String> followUpQuestions;

    @Schema(description = "답변과 관련된 팀 관행 카드 (관련 있는 게 없으면 빈 배열)")
    private final List<TeamNormCardResponse> relatedTeamNorms;

    // teamNormsById: 이번 요청에서 AI로 같이 보냈던 팀 관행 원본(카드에 category/content를 채우기 위함)
    public static QnaResponse from(AskResponse askResponse, Map<Long, TeamNorm> teamNormsById) {
        List<QnaSourceResponse> sources = askResponse.getSources().stream()
                .map(QnaSourceResponse::from)
                .toList();

        List<String> followUpQuestions = askResponse.getFollowUpQuestions() != null
                ? askResponse.getFollowUpQuestions()
                : List.of();

        List<TeamNormMatchDto> matches = askResponse.getRelatedTeamNorms() != null
                ? askResponse.getRelatedTeamNorms()
                : List.of();
        List<TeamNormCardResponse> relatedTeamNorms = matches.stream()
                .map(match -> {
                    TeamNorm norm = teamNormsById.get(match.getId());
                    if (norm == null) {
                        return null;
                    }
                    return TeamNormCardResponse.of(norm, match.getReason());
                })
                .filter(Objects::nonNull)
                .toList();

        return new QnaResponse(askResponse.getAnswer(), sources, followUpQuestions, relatedTeamNorms);
    }
}
