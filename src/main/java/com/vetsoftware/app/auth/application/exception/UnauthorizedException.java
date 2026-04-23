package com.vetsoftware.app.auth.application.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String permission) {
        super("Access denied: missing permission '" + permission + "'");
    }
}
