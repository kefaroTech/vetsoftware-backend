package com.vetsoftware.app.tax.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxResponse(
        Long id,
        String name,
        BigDecimal percentage,
        CompanySummary company,
        LocalDateTime createdDate,
        boolean enabled
) {}
