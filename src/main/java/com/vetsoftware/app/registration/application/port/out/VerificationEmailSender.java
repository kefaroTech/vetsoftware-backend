package com.vetsoftware.app.registration.application.port.out;

/**
 * Envia el correo de verificacion (plantilla de Resend) con el enlace de activacion. El usuario de
 * acceso es el propio correo, por lo que no se pasa aparte. La implementacion arma el enlace desde
 * la URL base y el token.
 */
public interface VerificationEmailSender {
  void send(String toEmail, String employeeName, String companyName, String rawToken);
}
