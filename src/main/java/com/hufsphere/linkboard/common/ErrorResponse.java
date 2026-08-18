package com.hufsphere.linkboard.common;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
