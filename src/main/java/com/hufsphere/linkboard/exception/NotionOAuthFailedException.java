package com.hufsphere.linkboard.exception;

public class NotionOAuthFailedException extends RuntimeException {

    public NotionOAuthFailedException(String message) {
        super(message);
    }
}
