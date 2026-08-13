package com.vetsoftware.app.deworming.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.deworming.domain.DewormingType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DewormingResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        LocalDate lastDeworming,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DewormingType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String product,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String dosage, LocalDate nextControl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String observations,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnimalSummary animal,
        ConsultationSummary consultation,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
