package com.vetsoftware.app.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Reporte del libro de compras (entradas) de una sede (o todas) en un rango, con totales. */
public record PurchasesReport(
        String branchName,
        LocalDate fromDate,
        LocalDate toDate,
        LocalDateTime generatedAt,
        List<PurchaseView> lines,
        int totalQuantity,
        BigDecimal totalValue
) {}
