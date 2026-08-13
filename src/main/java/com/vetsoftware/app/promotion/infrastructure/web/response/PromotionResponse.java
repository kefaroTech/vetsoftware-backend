package com.vetsoftware.app.promotion.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String promotionType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String applicationType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long applicationItem,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String valueType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal value,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String promotionStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
