package com.vetsoftware.app.daycare.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.daycare.domain.DayCareType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DayCareResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate startDate, LocalDate endDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DayCareType type, String objects,
        String observations,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnimalSummary animal,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
