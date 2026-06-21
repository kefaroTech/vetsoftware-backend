package com.vetsoftware.app.servicechargeopenaccount.application.command;

public record CreateServiceChargeOpenAccountCommand(
        Long animalId,
        Long serviceId,
        Long openAccountId,
        Long companyId,
        Long createdById,
        String clientRequestId,
        // Versión esperada de la cuenta (opt-in) para detección temprana de conflicto. null = sin chequeo.
        Long expectedVersion
) {}
