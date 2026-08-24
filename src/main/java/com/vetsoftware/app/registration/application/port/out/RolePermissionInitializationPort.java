package com.vetsoftware.app.registration.application.port.out;

/**
 * Copia a la empresa recien creada los permisos base del rol base indicado.
 *
 * <p>
 * Ya no recibe {@code membershipId}: el reparto no lo decide un plan sino el
 * contrato de la empresa, a traves de sus {@code company_entitlements}. El
 * adaptador resuelve por {@code companyId} los submodulos concedidos con nivel
 * {@code FULL} o {@code READ_ONLY} y filtra con ellos los permisos base.
 */
public interface RolePermissionInitializationPort {
    void initializeForRole(Long roleId, Long companyId, Long baseRoleId);
}
