package com.vetsoftware.app.inventory.application.dto;

import java.time.LocalDate;

/**
 * Lote próximo a vencer (o vencido), para alertas. {@code daysToExpire}
 * negativo = ya vencido.
 */
public record ExpiringLotView(Long productId, String productName, String productCode, Long branchId,
        String branchName, Long lotId, String lotNumber, LocalDate expireDate,
        int quantityAvailable, long daysToExpire) {
}
