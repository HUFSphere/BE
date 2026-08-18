package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.client.dto.AskResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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

    @Schema(description = "팀 관행 설명 (현재는 항상 null)", example = "null")
    private final String teamNorm;

    public static QnaResponse from(AskResponse askResponse) {
        List<QnaSourceResponse> sources = askResponse.getSources().stream()
                .map(QnaSourceResponse::from)
                .toList();
        return new QnaResponse(askResponse.getAnswer(), sources, null);
    }
}
