package com.vetsoftware.app.openaccount.application.command;

public record SearchOpenAccountsCommand(Long companyId, Long ownerId, Boolean enabled, int page,
        int pageSize,
        // Multi-sucursal (Fase C): filtro opcional por sede. null = todas las sedes de
        // la empresa.
        Long branchId) {
}
