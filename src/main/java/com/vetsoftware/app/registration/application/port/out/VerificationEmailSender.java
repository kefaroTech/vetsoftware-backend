package com.vetsoftware.app.registration.application.port.out;

/**
 * Envia el correo de verificacion (plantilla de Resend) con el enlace de activacion y el codigo de acceso
 * (employeeCode) generado. La implementacion arma el enlace a partir de la URL base configurada y el token.
 */
public interface VerificationEmailSender {
    void send(String toEmail, String employeeName, String companyName, String employeeCode, String rawToken);
}
