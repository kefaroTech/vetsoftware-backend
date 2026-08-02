package com.vetsoftware.app.auth.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cierra sesión del usuario actual: revoca sus refresh tokens e invalida sus
 * access vivos.
 */
public interface LogoutUseCase {
    @PreAuthorize("isAuthenticated()")
    void execute();
}
