package com.vetsoftware.app.auth.application.port.out;

import java.util.Set;

/**
 * Cruza los permisos asignados al empleado con el acceso contractual vigente de
 * su empresa. El resultado es deliberadamente por petición y no se cachea.
 */
public interface EffectivePermissionResolver {
    Set<String> resolveFor(Long companyId, Set<String> basePermissions);
}
