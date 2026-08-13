package com.vetsoftware.app.diagnosticimaging.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiagnosticImagingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DiagnosticImagingTypeSummary diagnosticImagingType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String clinicalSigns,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String studyType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String diagnosis,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String observations,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AnimalSummary animal,
        ConsultationSummary consultation,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
