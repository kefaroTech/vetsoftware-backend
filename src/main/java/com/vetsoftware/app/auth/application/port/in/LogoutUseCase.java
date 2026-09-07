package com.vetsoftware.app.auth.application.port.in;

import com.vetsoftware.app.auth.application.dto.AuthSubjectType;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cierra sesión del usuario actual: revoca sus refresh tokens e invalida sus
 * access vivos.
 */
public interface LogoutUseCase {
    /**
     * @return el tipo del sujeto cuya sesión se cerró, para que el controller borre
     *         solo la cookie de esa audiencia y no la de la otra app.
     */
    @PreAuthorize("isAuthenticated()")
    AuthSubjectType execute();
}
