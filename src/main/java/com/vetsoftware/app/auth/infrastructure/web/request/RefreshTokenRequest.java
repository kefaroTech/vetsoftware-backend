package com.vetsoftware.app.auth.infrastructure.web.request;

import com.vetsoftware.app.auth.application.dto.AuthSubjectType;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code POST /auth/refresh}. El refresh token en sí viaja en la
 * cookie {@code HttpOnly} correspondiente; este {@code type} es lo que le dice
 * al controller cuál de las dos cookies leer, porque su nombre depende del tipo
 * de sujeto.
 */
public record RefreshTokenRequest(@NotNull AuthSubjectType type) {
}
