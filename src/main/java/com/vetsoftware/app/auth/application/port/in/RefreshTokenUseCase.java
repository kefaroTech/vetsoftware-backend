package com.vetsoftware.app.auth.application.port.in;

import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Rota un refresh token válido: revoca el usado y emite un nuevo par access +
 * refresh.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es pública en PublicRoutes y la autorización es la credencial o el token de un solo uso que trae la propia petición.")
public interface RefreshTokenUseCase {
    TokenDto execute(String rawRefreshToken);
}
