package com.vetsoftware.app.auth.application.port.in;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

@NoAuthorizationRequired(reason = "Lo invoca el AuthFilter para construir el contexto de autenticación de la petición; exigir autorización aquí sería circular.")
public interface ResolveSystemAuthContextUseCase {
    AuthContext execute(Long systemUserId, Long authVersion);
}
