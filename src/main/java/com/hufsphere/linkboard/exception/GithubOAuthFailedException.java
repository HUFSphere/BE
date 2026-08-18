package com.hufsphere.linkboard.exception;

public class GithubOAuthFailedException extends RuntimeException {

    public GithubOAuthFailedException(String message) {
        super(message);
    }
}
