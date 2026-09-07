package com.vetsoftware.app.auth.application.port.in;

import com.vetsoftware.app.auth.application.dto.AuthSubjectType;
import com.vetsoftware.app.auth.application.dto.TokenDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Rota un refresh token válido: revoca el usado y emite un nuevo par access +
 * refresh.
 */
@NoAuthorizationRequired(reason = "Flujo previo a tener token: la ruta es pública en PublicRoutes y la autorización es la credencial o el token de un solo uso que trae la propia petición.")
public interface RefreshTokenUseCase {
    /**
     * @param expectedType
     *            tipo de sujeto que declara la cookie leída. Si no coincide con el
     *            tipo real del token almacenado, se rechaza: una cookie nunca se
     *            entrega a la audiencia de la otra app.
     */
    TokenDto execute(String rawRefreshToken, AuthSubjectType expectedType);
}
