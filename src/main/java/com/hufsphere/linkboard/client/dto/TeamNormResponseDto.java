package com.hufsphere.linkboard.client.dto;

import com.hufsphere.linkboard.domain.TeamNorm;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팀 관행 응답 DTO")
public class TeamNormResponseDto {

    @Schema(description = "관행 ID", example = "1")
    private Long id;

    @Schema(description = "관행 카테고리", example = "CODE_REVIEW")
    private String category;

    @Schema(description = "관행 내용 (1문장)", example = "최근 PR 12건 중 10건이 리뷰어 2명의 승인을 받은 뒤 머지되었습니다.")
    private String content;

    @Schema(description = "근거 기록 링크", example = "https://github.com/HUFSphere/BE/pull/16")
    private String evidenceUrl;

    @Schema(description = "근거 기록 제목", example = "feat: 인증·워크스페이스·소스 연동 반영")
    private String evidenceTitle;

    @Schema(description = "근거 기록의 소스 타입", example = "github")
    private String evidenceSourceType;

    @Schema(description = "생성(마지막 재생성) 일시", example = "2026-08-20T18:00:00")
    private LocalDateTime createdAt;

    public static TeamNormResponseDto from(TeamNorm norm) {
        return TeamNormResponseDto.builder()
                .id(norm.getId())
                .category(norm.getCategory())
                .content(norm.getContent())
                .evidenceUrl(norm.getEvidenceUrl())
                .evidenceTitle(norm.getEvidenceTitle())
                .evidenceSourceType(norm.getEvidenceSourceType())
                .createdAt(norm.getCreatedAt())
                .build();
    }
}
