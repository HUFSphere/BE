package com.hufsphere.linkboard.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class NotionTokenExchangeRequest {

    @JsonProperty("grant_type")
    private final String grantType = "authorization_code";

    private final String code;

    @JsonProperty("redirect_uri")
    private final String redirectUri;

    public NotionTokenExchangeRequest(String code, String redirectUri) {
        this.code = code;
        this.redirectUri = redirectUri;
    }
}
