package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductChargeOpenAccountResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnimalSummary animal,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProductSummary product,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal unitPrice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int quantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasTax,
        BigDecimal taxPercentage, String taxName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal baseAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal taxAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal totalAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OpenAccountSummary openAccount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EmployeeSummary createdBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean voided,
        EmployeeSummary voidedBy, LocalDateTime voidedAt, String voidReason) {
}
