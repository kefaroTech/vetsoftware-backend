package com.vetsoftware.app.passwordreset.application.port.in;

import com.vetsoftware.app.passwordreset.application.command.RequestPasswordResetCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Solicita el restablecimiento de contraseña. Sin @PreAuthorize: flujo público.
 * No revela si el código existe (anti-enumeración): siempre completa sin error,
 * envíe o no el correo.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es pública en PublicRoutes y la autorización es la credencial o el token de un solo uso que trae la propia petición.")
public interface RequestPasswordResetUseCase {
    void execute(RequestPasswordResetCommand command);
}
