package com.hufsphere.linkboard.client.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NotionIngestRequest {

    private final List<NotionPage> pages;
}
