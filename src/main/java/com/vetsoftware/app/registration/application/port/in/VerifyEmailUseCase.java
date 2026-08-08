package com.vetsoftware.app.registration.application.port.in;

import com.vetsoftware.app.registration.application.command.VerifyEmailCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es pública en PublicRoutes y la autorización es la credencial o el token de un solo uso que trae la propia petición.")
public interface VerifyEmailUseCase {
    void execute(VerifyEmailCommand command);
}
