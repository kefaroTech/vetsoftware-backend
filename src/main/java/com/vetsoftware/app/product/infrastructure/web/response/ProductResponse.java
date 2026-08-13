package com.vetsoftware.app.product.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.product.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal salePrice,
        String baseUnitMeasureCode, String provider, SupplierSummary supplier,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaxTreatment taxTreatment,
        String notes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductCategorySummary productCategory,
        TaxSummary tax, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        LocalDateTime updatedDate, Long updatedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
