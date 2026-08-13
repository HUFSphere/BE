package com.hufsphere.linkboard.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotionTokenExchangeResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("workspace_id")
    private String workspaceId;

    @JsonProperty("workspace_name")
    private String workspaceName;

    @JsonProperty("bot_id")
    private String botId;
}
