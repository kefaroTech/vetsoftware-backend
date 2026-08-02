package com.vetsoftware.app.passwordreset.application.port.out;

/**
 * Envía el correo de restablecimiento (plantilla de Resend) con el enlace que lleva el token plano.
 * La implementación arma el enlace desde la URL base del front y el token. Async/best-effort (no
 * bloquea).
 */
public interface PasswordResetEmailSender {
  void send(
      String toEmail,
      String employeeName,
      String employeeCode,
      String companyName,
      String rawToken);
}
