package com.vetsoftware.app.passwordreset.application.port.in;

import com.vetsoftware.app.passwordreset.application.command.ResetPasswordCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Confirma el restablecimiento: consume el token y cambia la contraseña del
 * empleado. Sin @PreAuthorize: la autorización es la posesión del token de un
 * solo uso.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es pública en PublicRoutes y la autorización es la credencial o el token de un solo uso que trae la propia petición.")
public interface ResetPasswordUseCase {
    void execute(ResetPasswordCommand command);
}
