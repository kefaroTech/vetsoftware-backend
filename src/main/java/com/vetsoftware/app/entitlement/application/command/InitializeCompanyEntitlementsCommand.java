package com.vetsoftware.app.entitlement.application.command;

/**
 * Derivar por primera vez los permisos de una empresa recien creada, dentro de
 * la transaccion de su alta.
 */
public record InitializeCompanyEntitlementsCommand(Long companyId) {
    public InitializeCompanyEntitlementsCommand {
        if (companyId == null)
            throw new IllegalArgumentException("company id is required");
    }
}
