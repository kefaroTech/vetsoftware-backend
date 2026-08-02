package com.vetsoftware.app.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Asiento del kardex para lectura (auditable). {@code quantity} con signo
 * (entrada +, salida −).
 */
public record StockMovementView(Long id, Long lotId, String type, int quantity, BigDecimal unitCost,
        String referenceType, Long referenceId, String reason, Long createdBy,
        LocalDateTime createdDate) {
}
