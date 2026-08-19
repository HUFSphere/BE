package com.hufsphere.linkboard.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AiChunkDto {

    @JsonProperty("source_type")
    private final String sourceType;

    @JsonProperty("item_type")
    private final String itemType;

    private final String title;
    private final String url;
    private final String text;
}
