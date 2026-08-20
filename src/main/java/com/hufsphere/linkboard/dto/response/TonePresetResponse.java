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

    @Schema(description = "프리셋 라벨", example = "간결하게")
    private final String label;

    @Schema(description = "프리셋이 실제로 AI에 전달하는 프롬프트 텍스트", example = "핵심만 간결하게 답변해주세요.")
    private final String description;

    public static TonePresetResponse from(TonePresets.TonePreset preset) {
        return new TonePresetResponse(preset.presetKey(), preset.label(), preset.description());
    }
}
