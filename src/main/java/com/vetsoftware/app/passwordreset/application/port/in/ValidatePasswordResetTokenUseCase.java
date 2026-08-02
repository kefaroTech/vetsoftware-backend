package com.vetsoftware.app.passwordreset.application.port.in;

/**
 * Indica si un token de restablecimiento sigue siendo usable (existe, no
 * expiró, no fue usado). No lo consume.
 */
public interface ValidatePasswordResetTokenUseCase {
    boolean isValid(String rawToken);
}
