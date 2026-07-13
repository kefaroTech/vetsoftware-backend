package com.vetsoftware.app.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila cruda del kardex para exportar (con nombres de producto/sede ya resueltos, orden ascendente por fecha).
 * El saldo corrido lo calcula el servicio; aquí {@code quantity} va con signo (entrada +, salida −).
 */
public record KardexExportRow(
        String productName,
        String productCode,
        String branchName,
        LocalDateTime createdDate,
        String type,
        String referenceType,
        Long referenceId,
        Long lotId,
        int quantity,
        BigDecimal unitCost
) {}
