package com.hufsphere.linkboard.client.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AskRequest {

    private final String question;
    private final String lang;
    private final String tone;
}
