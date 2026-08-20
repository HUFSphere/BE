package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.dto.TeamNormCandidateDto;
import com.hufsphere.linkboard.client.dto.TeamNormResponseDto;
import com.hufsphere.linkboard.domain.TeamNorm;
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

    // 소스 동기화마다 AI가 새로 분석한 팀 관행으로 워크스페이스 전체를 통째로 교체한다.
    // work item과 마찬가지로 요청을 보낸 소스 하나가 아니라 워크스페이스에 색인된 전체
    // (github+notion+figma)를 대상으로 재분석되므로, 소스 단위가 아니라 워크스페이스 단위로
    // 삭제 후 다시 채운다.
    @Transactional
    public void replaceForWorkspace(Long workspaceId, List<TeamNormCandidateDto> candidates) {
        teamNormRepository.deleteByWorkspaceId(workspaceId);

        for (TeamNormCandidateDto candidate : candidates) {
            TeamNorm norm = TeamNorm.builder()
                    .workspaceId(workspaceId)
                    .category(candidate.getCategory())
                    .content(candidate.getContent())
                    .evidenceUrl(candidate.getEvidenceUrl())
                    .evidenceTitle(candidate.getEvidenceTitle())
                    .evidenceSourceType(candidate.getEvidenceSourceType())
                    .build();
            teamNormRepository.save(norm);
        }
    }
}
