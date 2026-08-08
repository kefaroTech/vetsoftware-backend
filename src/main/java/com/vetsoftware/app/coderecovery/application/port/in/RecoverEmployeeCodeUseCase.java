package com.vetsoftware.app.coderecovery.application.port.in;

import com.vetsoftware.app.coderecovery.application.command.RecoverEmployeeCodeCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Envía por correo el/los código(s) de acceso asociados a un email.
 * Sin @PreAuthorize: flujo público. Anti-enumeración: siempre completa sin
 * error, encuentre o no cuentas.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es pública en PublicRoutes y la autorización es la credencial o el token de un solo uso que trae la propia petición.")
public interface RecoverEmployeeCodeUseCase {
    void execute(RecoverEmployeeCodeCommand command);
}
