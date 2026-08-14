package com.hufsphere.linkboard.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NotionPage {

    private final String title;
    private final String url;
    private final String text;

    @JsonProperty("item_type")
    private final String itemType;
}
