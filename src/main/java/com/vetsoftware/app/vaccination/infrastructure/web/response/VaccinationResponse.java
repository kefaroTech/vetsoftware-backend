package com.vetsoftware.app.vaccination.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VaccinationResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) VaccinationTypeSummary vaccinationType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String lot,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String notes, String route,
        String applicationSite, LocalDate nextVaccination,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnimalSummary animal,
        ConsultationSummary consultation,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
