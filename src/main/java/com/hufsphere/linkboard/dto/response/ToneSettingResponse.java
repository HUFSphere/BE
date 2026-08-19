package com.hufsphere.linkboard.dto.response;

import com.hufsphere.linkboard.common.TonePresets;
import com.hufsphere.linkboard.domain.ToneSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "톤 설정")
public class ToneSettingResponse {

    @Schema(description = "프리셋 키 목록 (중복 선택 가능)", example = "[\"beginner\", \"expert\"]")
    private final List<String> presetKeys;

    @Schema(description = "사용자 추가 텍스트", example = "특히 Spring 관련 결정은 더 자세히 설명해주세요")
    private final String customText;

    @Schema(description = "수정 시각. 저장된 설정이 없으면 null", example = "2026-08-19T10:00:00")
    private final LocalDateTime updatedAt;

    public static ToneSettingResponse from(ToneSetting toneSetting) {
        return new ToneSettingResponse(
                toneSetting.getPresetKeys(),
                toneSetting.getCustomText(),
                toneSetting.getUpdatedAt()
        );
    }

    public static ToneSettingResponse defaultResponse() {
        return new ToneSettingResponse(List.of(TonePresets.DEFAULT_PRESET_KEY), null, null);
    }
}
