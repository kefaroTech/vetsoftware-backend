package com.vetsoftware.app.auth.infrastructure.web.request;

/**
 * Cuerpo de {@code POST /auth/refresh}.
 *
 * <p>
 * El campo dejó de ser obligatorio: el refresh token viaja ahora en la cookie
 * {@code HttpOnly} {@code vet_refresh}. Se conserva sin {@code @NotBlank} —y el
 * cuerpo entero es opcional— para que un frontend todavía no desplegado siga
 * autenticándose mientras dura el despliegue coordinado. Cuando los dos fronts
 * estén arriba, este record y su rama en el controller se pueden borrar.
 */
public record RefreshTokenRequest(String refreshToken) {
}
