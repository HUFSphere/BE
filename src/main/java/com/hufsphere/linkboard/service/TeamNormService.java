package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.dto.TeamNormRequestDto;
import com.hufsphere.linkboard.client.dto.TeamNormResponseDto;
import com.hufsphere.linkboard.domain.TeamNorm;
import com.hufsphere.linkboard.exception.TeamNormNotFoundException;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.TeamNormRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamNormService {

    private final TeamNormRepository teamNormRepository;
    private final WorkspaceRepository workspaceRepository;

    // 6.1 팀 관행 목록 조회
    public List<TeamNormResponseDto> getNorms(Long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId);
        }

        return teamNormRepository.findByWorkspaceIdOrderByIdAsc(workspaceId).stream()
                .map(TeamNormResponseDto::from)
                .toList();
    }

    // 6.2 팀 관행 등록
    @Transactional
    public TeamNormResponseDto createNorm(Long workspaceId, TeamNormRequestDto request) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId);
        }

        TeamNorm norm = TeamNorm.builder()
                .workspaceId(workspaceId)
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL")
                .content(request.getContent())
                .build();

        TeamNorm saved = teamNormRepository.save(norm);
        return TeamNormResponseDto.from(saved);
    }

    // 6.3 팀 관행 수정
    @Transactional
    public TeamNormResponseDto updateNorm(Long workspaceId, Long normId, TeamNormRequestDto request) {
        TeamNorm norm = teamNormRepository.findByIdAndWorkspaceId(normId, workspaceId)
                .orElseThrow(() -> new TeamNormNotFoundException("해당 팀 관행을 찾을 수 없습니다. normId=" + normId));

        norm.update(request.getCategory(), request.getContent());
        return TeamNormResponseDto.from(norm);
    }

    // 6.4 팀 관행 삭제
    @Transactional
    public void deleteNorm(Long workspaceId, Long normId) {
        TeamNorm norm = teamNormRepository.findByIdAndWorkspaceId(normId, workspaceId)
                .orElseThrow(() -> new TeamNormNotFoundException("해당 팀 관행을 찾을 수 없습니다. normId=" + normId));

        teamNormRepository.delete(norm);
    }
}