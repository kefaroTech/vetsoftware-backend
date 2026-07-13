package com.vetsoftware.app.purchasereport.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Libro de compras (F4): una entrada por factura de proveedor del periodo + totales. Complementa el libro de ventas
 * fiscal (punto 16). Excluye las facturas anuladas.
 */
public record PurchaseBookDto(
        LocalDate dateFrom,
        LocalDate dateTo,
        List<EntryDto> entries,
        TotalsDto totals
) {
    public record EntryDto(
            Long id,
            String supplierName,
            String supplierTaxId,
            String invoiceNumber,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal withholdingAmount,
            BigDecimal total,
            BigDecimal paidAmount,
            BigDecimal balance,
            String status) {}

    public record TotalsDto(
            long invoiceCount,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal withholdingAmount,
            BigDecimal total,
            BigDecimal paidAmount,
            BigDecimal balance) {}
}
