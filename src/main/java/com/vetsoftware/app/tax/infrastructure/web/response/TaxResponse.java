package com.vetsoftware.app.tax.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.tax.domain.TaxScheme;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxResponse(Long id, String name, BigDecimal percentage, TaxScheme taxScheme,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        LocalDateTime updatedDate, Long updatedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
