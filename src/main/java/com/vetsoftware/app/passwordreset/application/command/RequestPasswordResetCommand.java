package com.vetsoftware.app.passwordreset.application.command;

/** Solicitud de restablecimiento de contraseña por código de empleado. */
public record RequestPasswordResetCommand(String employeeCode) {
}
