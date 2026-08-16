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

    @Schema(description = "워크스페이스 ID", example = "1")
    private Long workspaceId;

    @Schema(description = "관행 카테고리", example = "COMMUNICATION")
    private String category;

    @Schema(description = "관행 내용", example = "오후 6시 이후의 급한 연락은 슬랙 멘션으로 남겨주세요.")
    private String content;

    @Schema(description = "생성 일시", example = "2026-08-14T18:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시", example = "2026-08-14T18:00:00")
    private LocalDateTime updatedAt;

    public static TeamNormResponseDto from(TeamNorm norm) {
        return TeamNormResponseDto.builder()
                .id(norm.getId())
                .workspaceId(norm.getWorkspaceId())
                .category(norm.getCategory())
                .content(norm.getContent())
                .createdAt(norm.getCreatedAt())
                .updatedAt(norm.getUpdatedAt())
                .build();
    }
}