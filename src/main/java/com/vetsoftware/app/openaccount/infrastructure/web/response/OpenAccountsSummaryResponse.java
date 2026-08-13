package com.vetsoftware.app.openaccount.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record OpenAccountsSummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long openCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long closedCount,
        BigDecimal totalOutstanding) {
}
