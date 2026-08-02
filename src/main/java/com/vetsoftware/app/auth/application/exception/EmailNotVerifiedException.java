package com.vetsoftware.app.auth.application.exception;

/**
 * Login rechazado porque el empleado aun no verifico su correo (auto-registro Opcion B). Se mapea a
 * 403 con codigo EMAIL_NOT_VERIFIED para que el front ofrezca reenviar la verificacion. Lleva el
 * {@code identifier} (codigo de empleado intentado) para dejarlo en el log de auditoria.
 */
public class EmailNotVerifiedException extends RuntimeException {

  private final String identifier;

  public EmailNotVerifiedException(String identifier) {
    super("Email not verified");
    this.identifier = identifier;
  }

  /** Codigo de empleado con el que se intento iniciar sesion (no es un secreto). */
  public String getIdentifier() {
    return identifier;
  }
}
