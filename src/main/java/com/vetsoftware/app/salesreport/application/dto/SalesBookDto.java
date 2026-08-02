package com.vetsoftware.app.salesreport.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Libro/registro de ventas (F6): una entrada por documento + desglose de
 * impuesto por tarifa (insumo formulario 300) + recaudo por medio de pago
 * codificado DIAN + totales del periodo.
 */
public record SalesBookDto(LocalDate dateFrom, LocalDate dateTo, List<EntryDto> entries,
        List<TaxByRateDto> taxByRate, List<RecaudoDto> recaudoByMeans, TotalsDto totals) {
    public record EntryDto(Long id, String documentType, String prefix, Long consecutive,
            LocalDate issueDate, String customerDocumentId, String customerName, BigDecimal base,
            BigDecimal iva, BigDecimal inc, BigDecimal total, BigDecimal payable,
            BigDecimal reteFuente, BigDecimal reteIva, BigDecimal reteIca, String dianStatus,
            String cufe, String cude) {
    }

    public record TaxByRateDto(String taxScheme, BigDecimal taxRate, BigDecimal taxableAmount,
            BigDecimal taxAmount) {
    }

    public record RecaudoDto(String paymentMeans, String dianCode, BigDecimal amount) {
    }

    public record TotalsDto(long documentCount, BigDecimal base, BigDecimal iva, BigDecimal inc,
            BigDecimal total, BigDecimal payable, BigDecimal reteFuente, BigDecimal reteIva,
            BigDecimal reteIca) {
    }
}
