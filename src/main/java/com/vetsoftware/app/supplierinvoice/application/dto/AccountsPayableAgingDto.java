package com.vetsoftware.app.supplierinvoice.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de cuentas por pagar por antigüedad (aging). Agrupa el saldo pendiente de las facturas no pagadas por
 * proveedor y por tramo de días vencidos respecto a {@code asOf}: al día (no vencido), 1–30, 31–60, 61–90 y +90.
 */
public record AccountsPayableAgingDto(
        LocalDate asOf,
        List<SupplierRow> suppliers,
        Bucket totals
) {
    /** Fila por proveedor con su saldo repartido por tramo. */
    public record SupplierRow(
            Long supplierId,
            String supplierName,
            String taxId,
            Bucket bucket
    ) {}

    /** Montos por tramo de antigüedad + total. */
    public record Bucket(
            BigDecimal current,
            BigDecimal days1to30,
            BigDecimal days31to60,
            BigDecimal days61to90,
            BigDecimal over90,
            BigDecimal total
    ) {
        public static Bucket zero() {
            return new Bucket(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}
