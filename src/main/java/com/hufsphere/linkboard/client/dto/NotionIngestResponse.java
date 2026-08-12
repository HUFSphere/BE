package com.hufsphere.linkboard.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotionIngestResponse {

    private String source;
    private int indexed;
}
