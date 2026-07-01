package com.vetsoftware.app.product.infrastructure.web.response;

import com.vetsoftware.app.product.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String code,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        Integer currentStock,
        Integer minStock,
        String provider,
        TaxTreatment taxTreatment,
        LocalDate expireDate,
        String lotNumber,
        String notes,
        ProductCategorySummary productCategory,
        TaxSummary tax,
        CompanySummary company,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        Long updatedBy,
        Long version,
        boolean enabled
) {}
