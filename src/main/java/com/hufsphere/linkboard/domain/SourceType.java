package com.hufsphere.linkboard.domain;

import com.hufsphere.linkboard.exception.InvalidSourceTypeException;
import java.util.Arrays;

public enum SourceType {
    GITHUB("github"),
    NOTION("notion"),
    FIGMA("figma");

    private final String value;

    SourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SourceType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new InvalidSourceTypeException("지원하지 않는 sourceType입니다 (github, notion, figma)"));
    }
}
