package com.vetsoftware.app.passwordreset.application.port.in;

import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Indica si un token de restablecimiento sigue siendo usable (existe, no
 * expiró, no fue usado). No lo consume.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es pública en PublicRoutes y la autorización es la credencial o el token de un solo uso que trae la propia petición.")
public interface ValidatePasswordResetTokenUseCase {
    boolean isValid(String rawToken);
}
