package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.common.TonePresets;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "톤 프리셋")
public class TonePresetResponse {

    @Schema(description = "프리셋 키. 톤 설정 저장 시 presetKeys에 0개 이상 담아 보낸다", example = "concise")
    private final String presetKey;

    @Schema(description = "프리셋 라벨", example = "이 팀이 처음이에요")
    private final String label;

    public static TonePresetResponse from(TonePresets.TonePreset preset) {
        return new TonePresetResponse(preset.presetKey(), preset.label());
    }
}
