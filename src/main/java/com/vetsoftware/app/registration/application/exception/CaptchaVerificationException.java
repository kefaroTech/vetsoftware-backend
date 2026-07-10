package com.vetsoftware.app.registration.application.exception;

public class CaptchaVerificationException extends RuntimeException {
    public CaptchaVerificationException(String message) {
        super(message);
    }
}
