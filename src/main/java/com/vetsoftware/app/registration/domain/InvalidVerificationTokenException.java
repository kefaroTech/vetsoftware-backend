package com.vetsoftware.app.registration.domain;

public class InvalidVerificationTokenException extends RuntimeException {
  public InvalidVerificationTokenException(String message) {
    super(message);
  }
}
