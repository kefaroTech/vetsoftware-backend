package com.vetsoftware.app.breed.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SpecieSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
