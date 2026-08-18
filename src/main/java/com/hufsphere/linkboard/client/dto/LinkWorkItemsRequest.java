package com.hufsphere.linkboard.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LinkWorkItemsRequest {

    private final String lang;

    @JsonProperty("top_k")
    private final int topK;
}
