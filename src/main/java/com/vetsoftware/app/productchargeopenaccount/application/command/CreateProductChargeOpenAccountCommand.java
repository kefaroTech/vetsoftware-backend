package com.vetsoftware.app.productchargeopenaccount.application.command;

public record CreateProductChargeOpenAccountCommand(Long animalId, Long productId, int quantity,
        Long openAccountId, Long companyId, Long createdById,
        // Sede que descuenta el inventario. Puede venir null (admin sin elegir) → el
        // service resuelve
        // la Principal.
        Long branchId, String clientRequestId,
        // Versión esperada de la cuenta (opt-in) para detección temprana de conflicto.
        // null = sin
        // chequeo.
        Long expectedVersion) {
}
