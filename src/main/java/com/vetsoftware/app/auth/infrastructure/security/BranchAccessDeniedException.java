package com.vetsoftware.app.auth.infrastructure.security;

import org.springframework.security.access.AccessDeniedException;

/**
 * El empleado autenticado intentó operar sobre una sede fuera de su alcance (no está en sus
 * {@code employee_branches} y no es admin). Subclase de {@link AccessDeniedException} para que el
 * {@code GlobalExceptionHandler} la distinga con un código propio ({@code BRANCH_NOT_ALLOWED}) del 403 genérico.
 */
public class BranchAccessDeniedException extends AccessDeniedException {
    public BranchAccessDeniedException(String message) {
        super(message);
    }
}
