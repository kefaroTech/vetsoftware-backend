package com.vetsoftware.app.passwordreset.domain;

/** El token de restablecimiento no existe, expiró o ya fue usado. */
public class InvalidPasswordResetTokenException extends RuntimeException {
  public InvalidPasswordResetTokenException(String message) {
    super(message);
  }
}
