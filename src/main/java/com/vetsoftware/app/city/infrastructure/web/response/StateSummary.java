package com.vetsoftware.app.city.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record StateSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
