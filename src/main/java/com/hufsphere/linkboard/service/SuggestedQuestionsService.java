package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.dto.SuggestQuestionsResponse;
import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.dto.response.SuggestedQuestionsResponse;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuggestedQuestionsService {

    private static final String DEFAULT_LANG = "ko";

    private final WorkspaceRepository workspaceRepository;
    private final AiServerClient aiServerClient;

    // 신규 합류자가 워크스페이스에 들어오자마자 뭘 물어보면 좋을지 바로 볼 수 있도록,
    // AI 서버의 /suggest-questions를 그때그때 호출한다(DB에 저장하지 않음 — team norms와
    // 달리 매번 새로 생성해도 되는 가벼운 추천이라 굳이 워크스페이스 단위로 캐싱하지 않는다).
    public SuggestedQuestionsResponse getSuggestedQuestions(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId));

        String lang = workspace.getDefaultLanguage() != null && !workspace.getDefaultLanguage().isBlank()
                ? workspace.getDefaultLanguage()
                : DEFAULT_LANG;

        SuggestQuestionsResponse aiResponse = aiServerClient.suggestQuestions(lang);
        return new SuggestedQuestionsResponse(aiResponse.getQuestions());
    }
}
