package com.vetsoftware.app.city.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CityResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StateSummary state, String daneCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
