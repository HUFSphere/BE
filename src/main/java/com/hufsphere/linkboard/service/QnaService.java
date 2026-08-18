package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.client.AiServerClient;
import com.hufsphere.linkboard.client.dto.AskResponse;
import com.hufsphere.linkboard.dto.request.QnaRequest;
import com.hufsphere.linkboard.dto.response.QnaResponse;
import com.hufsphere.linkboard.exception.WorkspaceNotFoundException;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final WorkspaceRepository workspaceRepository;
    private final AiServerClient aiServerClient;

    public QnaResponse ask(Long workspaceId, QnaRequest request) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("워크스페이스를 찾을 수 없습니다");
        }

        AskResponse askResponse = aiServerClient.ask(request.getQuestion(), request.getLang());
        return QnaResponse.from(askResponse);
    }
}
