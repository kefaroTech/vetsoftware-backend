package com.vetsoftware.app.productchargeopenaccount.application.command;

public record UpdateProductChargeOpenAccountCommand(
        Long id,
        Long animalId,
        Long productId,
        Long openAccountId,
        Long companyId,
        // Versión esperada de la cuenta (opt-in) para detección temprana de conflicto. null = sin chequeo.
        Long expectedVersion
) {}
