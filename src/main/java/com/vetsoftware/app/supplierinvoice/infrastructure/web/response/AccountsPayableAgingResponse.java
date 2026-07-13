package com.vetsoftware.app.supplierinvoice.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AccountsPayableAgingResponse(
        LocalDate asOf,
        List<SupplierRow> suppliers,
        Bucket totals
) {
    public record SupplierRow(Long supplierId, String supplierName, String taxId, Bucket bucket) {}

    public record Bucket(
            BigDecimal current,
            BigDecimal days1to30,
            BigDecimal days31to60,
            BigDecimal days61to90,
            BigDecimal over90,
            BigDecimal total
    ) {}
}
