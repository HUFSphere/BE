package com.hufsphere.linkboard.client.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FigmaComment {

    private final String frameName;
    private final String url;
    private final String text;
}
