package com.vetsoftware.app.productchargeopenaccount.application.command;

public record CreateProductChargeOpenAccountCommand(
        Long animalId,
        Long productId,
        Long openAccountId,
        Long companyId,
        Long createdById,
        String clientRequestId,
        // Versión esperada de la cuenta (opt-in) para detección temprana de conflicto. null = sin chequeo.
        Long expectedVersion
) {}
