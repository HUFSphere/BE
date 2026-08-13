package com.hufsphere.linkboard.service;

import com.hufsphere.linkboard.domain.Workspace;
import com.hufsphere.linkboard.dto.WorkspaceSettingResponse;
import com.hufsphere.linkboard.dto.request.WorkspaceUpdateRequest;
import com.hufsphere.linkboard.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceSettingResponse getWorkspaceSettings(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId));

        return WorkspaceSettingResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .defaultLanguage(workspace.getDefaultLanguage() != null ? workspace.getDefaultLanguage() : "ko")
                .updatedAt(workspace.getUpdatedAt() != null ? workspace.getUpdatedAt() : LocalDateTime.now())
                .build();
    }

    @Transactional
    public WorkspaceSettingResponse updateWorkspaceSettings(Long workspaceId, WorkspaceUpdateRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. id=" + workspaceId));

        if (request.getName() != null && !request.getName().isBlank()) {
            workspace.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workspace.setDescription(request.getDescription());
        }
        if (request.getDefaultLanguage() != null && !request.getDefaultLanguage().isBlank()) {
            workspace.setDefaultLanguage(request.getDefaultLanguage());
        }
        workspace.setUpdatedAt(LocalDateTime.now());

        return WorkspaceSettingResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .description(workspace.getDescription())
                .defaultLanguage(workspace.getDefaultLanguage())
                .updatedAt(workspace.getUpdatedAt())
                .build();
    }
}