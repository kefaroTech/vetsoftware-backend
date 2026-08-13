package com.vetsoftware.app.productcategory.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ProductCategoryResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        LocalDateTime updatedDate, Long updatedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
