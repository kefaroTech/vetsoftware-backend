package com.vetsoftware.app.owner.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CompanySummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String identifier) {
}
