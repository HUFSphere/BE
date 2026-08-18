package com.hufsphere.linkboard.exception;

public class WorkspaceOrUserNotFoundException extends RuntimeException {

    public WorkspaceOrUserNotFoundException(String message) {
        super(message);
    }
}