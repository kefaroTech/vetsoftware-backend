package com.vetsoftware.app.laboratorytest.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LaboratoryTestResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LaboratoryTestTypeSummary testType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer quantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String diagnosis,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String prioridad,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnimalSummary animal,
        ConsultationSummary consultation,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        EmployeeSummary processedBy, LocalDateTime processedDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
