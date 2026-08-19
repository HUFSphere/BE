package com.hufsphere.linkboard.common;

import java.util.Map;
import java.util.Optional;

// 톤 설정의 customText(주관식 입력)에 쓰인 문자 체계를 보고 언어를 추정한다.
// LLM에게 "이 문장이 무슨 언어냐" 판별을 맡기는 방식은 실제로 신뢰할 수 없었다(라이브 테스트로 확인 —
// 프리셋 설명 뒤에 짧게 덧붙인 다른 언어 문장은 거의 항상 무시되고 프리셋 언어로 답변이 나옴).
// 문자 체계가 서로 겹치지 않는 언어(한글/한자·가나/아랍/키릴)는 유니코드 블록만으로 결정적으로 판별 가능하므로
// BE에서 직접 감지해서 lang 자체를 바꿔 보낸다. 로마자 계열 언어(en/de/es/ms/it/fr)는 유니코드 블록으로
// 서로 구분이 안 되므로 감지 대상에서 제외한다(기존처럼 사용자가 명시한 lang을 그대로 씀).
public final class LanguageScriptDetector {

    private static final Map<String, int[][]> SCRIPT_RANGES = Map.of(
            "ko", new int[][] {{0xAC00, 0xD7A3}},           // 한글 음절
            "ja", new int[][] {{0x3040, 0x30FF}},           // 히라가나 + 가타카나
            "zh", new int[][] {{0x4E00, 0x9FFF}},           // 한자(CJK 통합 한자)
            "ar", new int[][] {{0x0600, 0x06FF}, {0x0750, 0x077F}}, // 아랍 문자
            "ru", new int[][] {{0x0400, 0x04FF}}            // 키릴 문자
    );

    // 우연히 섞여 들어간 한두 글자로 오탐하지 않도록 최소 등장 횟수를 요구한다.
    private static final int MIN_MATCH_COUNT = 3;

    private LanguageScriptDetector() {
    }

    public static Optional<String> detect(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String bestLang = null;
        int bestCount = 0;
        for (Map.Entry<String, int[][]> entry : SCRIPT_RANGES.entrySet()) {
            int count = countMatches(text, entry.getValue());
            if (count > bestCount) {
                bestCount = count;
                bestLang = entry.getKey();
            }
        }

        return bestCount >= MIN_MATCH_COUNT ? Optional.of(bestLang) : Optional.empty();
    }

    private static int countMatches(String text, int[][] ranges) {
        int count = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            for (int[] range : ranges) {
                if (codePoint >= range[0] && codePoint <= range[1]) {
                    count++;
                    break;
                }
            }
            i += Character.charCount(codePoint);
        }
        return count;
    }
}
