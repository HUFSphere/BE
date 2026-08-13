package com.hufsphere.linkboard.client.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NotionCrawledPage {

    private final String id;
    private final String title;
    private final String url;
}
