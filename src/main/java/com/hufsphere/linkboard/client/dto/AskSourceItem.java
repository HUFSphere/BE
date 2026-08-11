package com.hufsphere.linkboard.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AskSourceItem {

    @JsonProperty("source_type")
    private String sourceType;

    @JsonProperty("item_type")
    private String itemType;

    private String title;
    private String url;
}
