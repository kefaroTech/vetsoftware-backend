package com.vetsoftware.app.registration.application.port.out;

/**
 * Envia el correo de verificacion con el enlace de activacion. La implementacion arma el enlace final a
 * partir de la URL base configurada y el token plano.
 */
public interface VerificationEmailSender {
    void send(String toEmail, String employeeName, String rawToken);
}
