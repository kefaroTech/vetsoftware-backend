package com.vetsoftware.app.entitlement.application.command;

/**
 * Recalcular los permisos de una empresa desde su contrato vigente.
 *
 * <p>
 * La empresa <strong>no viaja en el cuerpo de ninguna peticion REST</strong>:
 * la pone el controller con {@code authz.currentCompanyId()} y el puerto la
 * revalida. El command la lleva porque el caso de uso la necesita para llegar
 * al dominio y porque es lo que hace comprobable la regla de tenant.
 */
public record RecalculateCompanyEntitlementsCommand(Long companyId) {
    public RecalculateCompanyEntitlementsCommand {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
    }
}
