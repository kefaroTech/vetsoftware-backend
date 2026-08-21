package com.vetsoftware.app.openaccount.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OpenAccountResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OwnerSummary owner,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal totalAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal paidAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal outstandingAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OpenAccountBranchSummary branch,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OpenAccountStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OpenAccountEmployeeSummary createdBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        OpenAccountEmployeeSummary closedBy, LocalDateTime closedAt, String closeReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean reversed,
        LocalDateTime reversedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version) {
}
