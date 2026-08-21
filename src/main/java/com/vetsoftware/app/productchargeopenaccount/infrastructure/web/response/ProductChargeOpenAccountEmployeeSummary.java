package com.vetsoftware.app.productchargeopenaccount.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductChargeOpenAccountEmployeeSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
