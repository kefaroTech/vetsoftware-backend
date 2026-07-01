package com.vetsoftware.app.tax.infrastructure.web.response;

import com.vetsoftware.app.tax.domain.TaxScheme;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxResponse(
        Long id,
        String name,
        BigDecimal percentage,
        TaxScheme taxScheme,
        CompanySummary company,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        Long updatedBy,
        Long version,
        boolean enabled
) {}
