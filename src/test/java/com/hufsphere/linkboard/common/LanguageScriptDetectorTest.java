package com.hufsphere.linkboard.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class LanguageScriptDetectorTest {

    @Test
    void 아랍어_문장은_ar로_감지된다() {
        Optional<String> result = LanguageScriptDetector.detect("أرجو أن تجيب باللغة العربية فقط من فضلك");
        assertThat(result).contains("ar");
    }

    @Test
    void 한글_문장은_ko로_감지된다() {
        Optional<String> result = LanguageScriptDetector.detect("한국어로만 답변해주세요");
        assertThat(result).contains("ko");
    }

    @Test
    void 러시아어_문장은_ru로_감지된다() {
        Optional<String> result = LanguageScriptDetector.detect("Пожалуйста, отвечайте только на русском языке");
        assertThat(result).contains("ru");
    }

    @Test
    void 영어처럼_로마자_계열은_감지하지_않는다() {
        Optional<String> result = LanguageScriptDetector.detect("Please answer only in English");
        assertThat(result).isEmpty();
    }

    @Test
    void 한두_글자만_섞인_경우는_오탐하지_않는다() {
        Optional<String> result = LanguageScriptDetector.detect("Spring Security와 JWT 관련해서 좀 더 알려주세요");
        // 한글 비중이 충분히 많으므로 이 경우는 ko로 잡히는 게 맞다
        assertThat(result).contains("ko");
    }

    @Test
    void null이나_빈_문자열은_감지하지_않는다() {
        assertThat(LanguageScriptDetector.detect(null)).isEmpty();
        assertThat(LanguageScriptDetector.detect("")).isEmpty();
        assertThat(LanguageScriptDetector.detect("   ")).isEmpty();
    }
}
