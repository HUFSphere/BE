package com.hufsphere.linkboard.client.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FigmaIngestRequest {

    private final List<FigmaComment> comments;
}
