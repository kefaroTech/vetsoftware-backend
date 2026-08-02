package com.vetsoftware.app.auth.application.exception;

/**
 * La versión del token ya no corresponde a la única sesión activa de la cuenta.
 */
public class SessionReplacedException extends RuntimeException {

    public SessionReplacedException() {
        super("Tu cuenta se inició en otro dispositivo.");
    }
}
