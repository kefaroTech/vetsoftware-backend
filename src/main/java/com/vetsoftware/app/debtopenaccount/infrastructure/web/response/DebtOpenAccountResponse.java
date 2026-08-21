package com.vetsoftware.app.debtopenaccount.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DebtOpenAccountResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaymentMethod paymentMethod,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OpenAccountSummary openAccount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DebtOpenAccountEmployeeSummary createdBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean voided,
        DebtOpenAccountEmployeeSummary voidedBy, LocalDateTime voidedAt, String voidReason) {
}
