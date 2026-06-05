package com.vetsoftware.app.service.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceResponse(
        Long id,
        String name,
        BigDecimal price,
        boolean hasTax,
        String notes,
        ServiceCategorySummary serviceCategory,
        TaxSummary tax,
        CompanySummary company,
        LocalDateTime createdDate,
        boolean enabled
) {}
