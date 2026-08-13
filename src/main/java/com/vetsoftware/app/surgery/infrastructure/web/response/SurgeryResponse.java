package com.vetsoftware.app.surgery.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SurgeryResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SurgeryTypeSummary surgeryType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String medicament,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String observations,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String complications,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnimalSummary animal,
        ConsultationSummary consultation,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
