package com.vetsoftware.app.debtopenaccount.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record DebtOpenAccountEmployeeSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
