package com.vetsoftware.app.inventory.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporte de kardex de un producto en una sede (o todas), con saldo
 * inicial/final y saldo corrido por línea.
 */
public record KardexReport(String productName, String productCode, String branchName,
        LocalDate fromDate, LocalDate toDate, LocalDateTime generatedAt, int openingBalance,
        int closingBalance, List<KardexReportLine> lines) {
}
