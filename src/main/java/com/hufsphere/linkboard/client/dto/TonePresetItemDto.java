package com.hufsphere.linkboard.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TonePresetItemDto {

    private String presetKey;
    private String label;
    private String description;
}
