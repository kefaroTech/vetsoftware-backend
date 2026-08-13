package com.vetsoftware.app.consultation.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConsultationResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ConsultationTypeSummary consultationType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String anamnesis, String diagnosis,
        String prognosis, LocalDate nextControl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnimalSummary animal,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        BigDecimal temperature, Integer heartRate, Integer respiratoryRate, String mucousMembranes,
        String capillaryRefill, String hydration, Integer bodyConditionScore, Integer painScore,
        String attitude, String examFindings) {
}
