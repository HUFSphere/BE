package com.hufsphere.linkboard.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 톤 프리셋은 AI 서버 호출 없이 BE가 즉시 반환해야 하므로 DB가 아닌 코드에서 하드코딩으로 관리한다.
// 프리셋은 답변 스타일(간결/자세/친근함) 기준이고, 다른 UI 요소들과 마찬가지로 한국어/영어 두 언어만
// 지원한다(그 외 언어 요청은 영어로 대체). 신입/중급/숙련 같은 경험치 기준 프리셋은 폐지했다.
public final class TonePresets {

    private static final String FALLBACK_LANG = "en";

    private TonePresets() {
    }

    public record TonePreset(String presetKey, String label, String description) {
    }

    private static final Map<String, Map<String, TonePreset>> PRESETS_BY_LANG = buildPresets();

    public static List<TonePreset> getPresets(String lang) {
        return List.copyOf(presetsFor(lang).values());
    }

    public static boolean isValidPresetKey(String presetKey) {
        return presetsFor(FALLBACK_LANG).containsKey(presetKey);
    }

    public static String getDescription(String presetKey, String lang) {
        Map<String, TonePreset> byLang = presetsFor(lang);
        TonePreset preset = byLang.get(presetKey);
        if (preset == null) {
            preset = presetsFor(FALLBACK_LANG).get(presetKey);
        }
        return preset != null ? preset.description() : "";
    }

    private static Map<String, TonePreset> presetsFor(String lang) {
        return PRESETS_BY_LANG.getOrDefault(lang, PRESETS_BY_LANG.get(FALLBACK_LANG));
    }

    private static Map<String, Map<String, TonePreset>> buildPresets() {
        Map<String, Map<String, TonePreset>> presets = new LinkedHashMap<>();

        presets.put("ko", ofPresets(
                new TonePreset("concise", "간결하게", "핵심만 간결하게 답변해주세요."),
                new TonePreset("detailed", "자세하게", "배경과 이유를 자세히 설명해주세요."),
                new TonePreset("friendly", "친근하게", "친근하고 편안한 말투로 답변해주세요.")
        ));

        presets.put("en", ofPresets(
                new TonePreset("concise", "Concise", "Please answer concisely, focusing on the key points."),
                new TonePreset("detailed", "Detailed", "Please explain the background and reasoning in detail."),
                new TonePreset("friendly", "Friendly", "Please answer in a friendly, approachable tone.")
        ));

        return Map.copyOf(presets);
    }

    private static Map<String, TonePreset> ofPresets(TonePreset... presets) {
        Map<String, TonePreset> map = new LinkedHashMap<>();
        for (TonePreset preset : presets) {
            map.put(preset.presetKey(), preset);
        }
        return map;
    }
}
