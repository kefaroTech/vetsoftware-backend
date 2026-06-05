package com.vetsoftware.app.product.infrastructure.web.response;

import java.math.BigDecimal;
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
        boolean hasTax,
        boolean expireDate,
        String notes,
        ProductCategorySummary productCategory,
        TaxSummary tax,
        CompanySummary company,
        LocalDateTime createdDate,
        boolean enabled
) {}
