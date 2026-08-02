package com.vetsoftware.app.servicechargeopenaccount.application.command;

public record UpdateServiceChargeOpenAccountCommand(Long id, Long animalId, Long serviceId,
        Long openAccountId, Long companyId,
        // Versión esperada de la cuenta (opt-in) para detección temprana de conflicto.
        // null = sin
        // chequeo.
        Long expectedVersion) {
}
