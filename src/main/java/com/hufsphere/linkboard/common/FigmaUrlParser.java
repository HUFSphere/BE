package com.hufsphere.linkboard.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 사용자가 Figma에서 "Share -> Copy link"로 복사해온 URL에서 fileKey만 뽑아낸다.
// FigmaCrawlerClient는 순수 fileKey를 그대로 API 경로(/v1/files/{fileKey}/...)에 꽂아 쓰므로,
// 어디서 값을 저장하든 이 파서를 거쳐야 한다.
public final class FigmaUrlParser {

    // /file/{key}(구 URL), /design/{key}(신규 URL), /proto/{key}(프로토타입 링크) 모두 지원한다.
    private static final Pattern FIGMA_FILE_URL_PATTERN =
            Pattern.compile("figma\\.com/(?:file|design|proto)/([a-zA-Z0-9]+)");

    private FigmaUrlParser() {
    }

    // Figma 공유 링크면 URL에서 fileKey를 추출하고, 이미 순수 fileKey(또는 그 외 값)면 그대로 반환한다.
    public static String extractFileKey(String urlOrKey) {
        if (urlOrKey == null) {
            return null;
        }

        String trimmed = urlOrKey.trim();
        Matcher matcher = FIGMA_FILE_URL_PATTERN.matcher(trimmed);
        return matcher.find() ? matcher.group(1) : trimmed;
    }
}
