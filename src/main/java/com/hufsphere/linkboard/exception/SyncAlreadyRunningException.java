package com.hufsphere.linkboard.exception;

public class SyncAlreadyRunningException extends RuntimeException {

    public SyncAlreadyRunningException(String message) {
        super(message);
    }
}
