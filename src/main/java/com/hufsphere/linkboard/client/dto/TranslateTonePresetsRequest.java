package com.hufsphere.linkboard.client.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TranslateTonePresetsRequest {

    private final String lang;
    private final List<TonePresetItemDto> presets;
}
