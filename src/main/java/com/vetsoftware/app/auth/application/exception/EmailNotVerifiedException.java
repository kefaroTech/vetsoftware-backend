package com.vetsoftware.app.auth.application.exception;

/**
 * Login rechazado porque el empleado aun no verifico su correo (auto-registro Opcion B).
 * Se mapea a 403 con codigo EMAIL_NOT_VERIFIED para que el front ofrezca reenviar la verificacion.
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Email not verified");
    }
}
