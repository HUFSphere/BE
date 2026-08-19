package com.hufsphere.linkboard.client.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TeamNormInputDto {

    private final Long id;
    private final String content;
}
