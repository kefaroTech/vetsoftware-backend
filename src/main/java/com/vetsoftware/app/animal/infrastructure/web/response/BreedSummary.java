package com.vetsoftware.app.animal.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record BreedSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
