package com.vetsoftware.app.inventory.application.command;

import java.math.BigDecimal;

/**
 * Ajuste de inventario (conteo físico). {@code delta} con signo: + entra, −
 * sale. {@code unitCost} aplica solo a los ajustes de entrada (delta > 0); si
 * es null se usa 0. {@code reason} obligatorio.
 */
public record RecordAdjustmentCommand(Long companyId, Long branchId, Long productId, int delta,
        BigDecimal unitCost, String reason, Long referenceId, Long createdBy) {
}
