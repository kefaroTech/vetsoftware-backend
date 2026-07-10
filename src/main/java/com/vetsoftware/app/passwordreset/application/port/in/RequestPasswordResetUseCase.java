package com.vetsoftware.app.passwordreset.application.port.in;

import com.vetsoftware.app.passwordreset.application.command.RequestPasswordResetCommand;

/**
 * Solicita el restablecimiento de contraseña. Sin @PreAuthorize: flujo público. No revela si el código existe
 * (anti-enumeración): siempre completa sin error, envíe o no el correo.
 */
public interface RequestPasswordResetUseCase {
    void execute(RequestPasswordResetCommand command);
}
