package com.vetsoftware.app.auth.application.port.out;

/**
 * Emite un refresh token nuevo para un sujeto (empleado o system user): genera
 * el valor opaco, persiste su hash con la expiración configurada y devuelve el
 * valor en claro.
 */
public interface RefreshTokenIssuer {
    String issue(Long subjectId, String subjectType, Long authVersion);
}
