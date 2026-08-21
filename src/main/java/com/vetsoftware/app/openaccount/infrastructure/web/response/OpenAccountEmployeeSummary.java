package com.vetsoftware.app.openaccount.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record OpenAccountEmployeeSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
