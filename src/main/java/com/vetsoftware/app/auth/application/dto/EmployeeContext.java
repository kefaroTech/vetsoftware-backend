package com.vetsoftware.app.auth.application.dto;

import java.util.Set;

/**
 * Identidad y autorización del empleado autenticado, resuelta por request desde
 * el JWT.
 *
 * <p>
 * {@code branchIds} es el alcance por sede (multi-sucursal): los recursos
 * scopeados a sede se limitan a esas sedes. "Todas las sedes" se materializa
 * como una fila por sede en {@code
 * employee_branches} (no hay flag). Un set vacío = sin acceso a recursos
 * scopeados a sede. Todos los empleados, incluido el rol empresarial
 * {@code ADMIN}, se autorizan exclusivamente mediante permisos granulares y
 * alcance tenant.
 */
public record EmployeeContext(Long employeeId, Long companyId, Set<String> permissions,
        Set<Long> branchIds) implements AuthContext {
}
