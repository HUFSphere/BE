package com.hufsphere.linkboard.domain;

import java.util.Arrays;

public enum ConnStatus {
    PENDING("pending"),
    COLLECTING("collecting"),
    INDEXING("indexing"),
    SUMMARIZING("summarizing"),
    DONE("done"),
    FAILED("failed");

    private final String value;

    ConnStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConnStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 connStatus 값입니다: " + value));
    }
}
