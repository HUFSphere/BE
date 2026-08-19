package com.hufsphere.linkboard.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FigmaUrlParserTest {

    @Test
    void 구_URL_포맷에서_fileKey를_추출한다() {
        String key = FigmaUrlParser.extractFileKey("https://www.figma.com/file/AbC123XyZ/My-Project?node-id=1-2");

        assertThat(key).isEqualTo("AbC123XyZ");
    }

    @Test
    void 신규_design_URL_포맷에서_fileKey를_추출한다() {
        String key = FigmaUrlParser.extractFileKey(
                "https://www.figma.com/design/AbC123XyZ/My-Project?node-id=1-2&t=abcd-1");

        assertThat(key).isEqualTo("AbC123XyZ");
    }

    @Test
    void 프로토타입_링크에서도_fileKey를_추출한다() {
        String key = FigmaUrlParser.extractFileKey("https://www.figma.com/proto/AbC123XyZ/My-Project");

        assertThat(key).isEqualTo("AbC123XyZ");
    }

    @Test
    void 이미_순수_fileKey면_그대로_반환한다() {
        String key = FigmaUrlParser.extractFileKey("AbC123XyZ");

        assertThat(key).isEqualTo("AbC123XyZ");
    }

    @Test
    void 앞뒤_공백은_제거한다() {
        String key = FigmaUrlParser.extractFileKey("  AbC123XyZ  ");

        assertThat(key).isEqualTo("AbC123XyZ");
    }

    @Test
    void null이면_null을_반환한다() {
        assertThat(FigmaUrlParser.extractFileKey(null)).isNull();
    }
}
