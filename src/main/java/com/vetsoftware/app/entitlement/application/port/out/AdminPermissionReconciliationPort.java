package com.vetsoftware.app.entitlement.application.port.out;

import java.time.LocalDateTime;

/**
 * Materializa en el rol ADMIN la proyeccion del contrato recien recalculado.
 */
public interface AdminPermissionReconciliationPort {
    void reconcile(Long companyId, LocalDateTime recalculatedAt);
}
