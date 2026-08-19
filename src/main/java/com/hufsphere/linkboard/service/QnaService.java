package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.dto.AiChunkDto;
import com.hufsphere.linkboard.client.dto.AskResponse;
import com.hufsphere.linkboard.client.dto.TeamNormInputDto;
import com.hufsphere.linkboard.domain.TeamNorm;
import com.hufsphere.linkboard.domain.WorkItem;
import com.hufsphere.linkboard.dto.request.QnaRequest;
import com.hufsphere.linkboard.dto.response.QnaResponse;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.TeamNormRepository;
import com.hufsphere.linkboard.repository.WorkItemRepository;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkItemRepository workItemRepository;
    private final TeamNormRepository teamNormRepository;
    private final AiServerClient aiServerClient;
    private final ToneSettingService toneSettingService;

    public QnaResponse ask(Long workspaceId, Long requesterId, QnaRequest request) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다");
        }

        String tone = toneSettingService.resolveToneText(workspaceId, requesterId, request.getLang());

        Map<Long, TeamNorm> teamNormsById = teamNormRepository.findByWorkspaceIdOrderByIdAsc(workspaceId).stream()
                .collect(Collectors.toMap(TeamNorm::getId, Function.identity()));
        List<TeamNormInputDto> teamNorms = teamNormsById.values().stream()
                .map(norm -> new TeamNormInputDto(norm.getId(), norm.getContent()))
                .toList();

        List<Long> contextWorkItemIds = request.getContextWorkItemIds();
        AskResponse askResponse;
        if (contextWorkItemIds != null && !contextWorkItemIds.isEmpty()) {
            // 지정된 work item으로만 답변 범위를 좁힌다. /ask(전역 임베딩 검색) 대신
            // chunks를 직접 넘기는 /qna를 호출한다.
            List<AiChunkDto> chunks = workItemRepository.findByIdInAndWorkspaceId(contextWorkItemIds, workspaceId).stream()
                    .map(this::toChunk)
                    .toList();
            askResponse = aiServerClient.qna(request.getQuestion(), request.getLang(), tone, teamNorms, chunks);
        } else {
            askResponse = aiServerClient.ask(request.getQuestion(), request.getLang(), tone, teamNorms);
        }

        return QnaResponse.from(askResponse, teamNormsById);
    }

    // WorkItem엔 AI가 채워준 적 없는 content(항상 null)만 있고 원본 chunk 전문은 저장돼 있지 않아서,
    // title + summaryNative(AI가 만든 1문장 요약)로 대신한다. 원본 근거 전문 기반 스코핑이 필요하면
    // AI가 work item마다 원본 텍스트를 persist하도록 만드는 별도 작업이 필요하다.
    private AiChunkDto toChunk(WorkItem item) {
        String text = item.getSummaryNative() != null ? item.getSummaryNative() : "";
        return new AiChunkDto(
                item.getSourceType() != null ? item.getSourceType().getValue() : "",
                item.getItemType(),
                item.getTitle(),
                item.getSourceUrl(),
                text
        );
    }
}
