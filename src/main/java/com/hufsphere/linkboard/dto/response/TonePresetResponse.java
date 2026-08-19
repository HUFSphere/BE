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

    @Schema(description = "프리셋 키", example = "beginner")
    private final String presetKey;

    @Schema(description = "프리셋 라벨", example = "이 팀이 처음이에요")
    private final String label;

    @Schema(description = "프리셋 설명(Q&A 톤에 반영되는 문구)", example = "저는 새로 합류한 주니어입니다. 기술 결정의 이유와 배경을 자세히 설명해주세요...")
    private final String description;

    public static TonePresetResponse from(TonePresets.TonePreset preset) {
        return new TonePresetResponse(preset.presetKey(), preset.label(), preset.description());
    }
}
