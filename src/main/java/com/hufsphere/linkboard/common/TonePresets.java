package com.hufsphere.linkboard.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 톤 프리셋은 AI 서버 호출 없이 BE가 즉시 반환해야 하므로 DB가 아닌 코드에서 하드코딩으로 관리한다.
// 언어별 프리셋이 없으면(ko/en/vi 외) en으로 대체한다.
public final class TonePresets {

    public static final String DEFAULT_PRESET_KEY = "beginner";
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
            preset = presetsFor(FALLBACK_LANG).get(DEFAULT_PRESET_KEY);
        }
        return preset.description();
    }

    private static Map<String, TonePreset> presetsFor(String lang) {
        return PRESETS_BY_LANG.getOrDefault(lang, PRESETS_BY_LANG.get(FALLBACK_LANG));
    }

    private static Map<String, Map<String, TonePreset>> buildPresets() {
        Map<String, Map<String, TonePreset>> presets = new LinkedHashMap<>();

        presets.put("ko", ofPresets(
                new TonePreset("beginner", "이 팀이 처음이에요",
                        "저는 새로 합류한 주니어입니다. 기술 결정의 이유와 배경을 자세히 설명해주세요. 팀의 관행이나 암묵적 규칙도 함께 알려주시면 좋겠습니다."),
                new TonePreset("intermediate", "어느 정도 알아요",
                        "개발 경험은 있지만 이 프로젝트는 처음입니다. 핵심 결정 사항과 팀 고유 관행 위주로 알려주세요."),
                new TonePreset("expert", "숙련자예요",
                        "경험 많은 개발자입니다. 이 팀만의 특수한 결정과 주의점만 간결하게 알려주세요.")
        ));

        presets.put("en", ofPresets(
                new TonePreset("beginner", "I'm new to this team",
                        "I'm a junior developer who just joined. Please explain technical decisions in detail with context and background. Include any team conventions or implicit rules."),
                new TonePreset("intermediate", "I have some experience",
                        "I have development experience but this project is new to me. Focus on key decisions and team-specific practices."),
                new TonePreset("expert", "I'm experienced",
                        "I'm an experienced developer. Just tell me what's unique about this team's decisions and what to watch out for, concisely.")
        ));

        presets.put("vi", ofPresets(
                new TonePreset("beginner", "Tôi mới vào team",
                        "Tôi là lập trình viên mới. Xin hãy giải thích chi tiết lý do đằng sau các quyết định kỹ thuật, bao gồm cả bối cảnh và quy tắc ngầm của team."),
                new TonePreset("intermediate", "Tôi có kinh nghiệm",
                        "Tôi có kinh nghiệm phát triển nhưng dự án này là mới. Hãy tập trung vào các quyết định quan trọng và thông lệ riêng của team."),
                new TonePreset("expert", "Tôi là chuyên gia",
                        "Tôi là lập trình viên giàu kinh nghiệm. Chỉ cần nói ngắn gọn những quyết định đặc biệt và điểm cần lưu ý của team này.")
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
