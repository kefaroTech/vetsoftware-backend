package com.vetsoftware.app.passwordreset.application.command;

/** Confirmación del restablecimiento: token del correo + nueva contraseña. */
public record ResetPasswordCommand(String token, String newPassword) {}
