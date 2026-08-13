package com.vetsoftware.app.hospitalizationmedication.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record HospitalizationMedicationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, String dose,
        String frequency, String guidelineType, String durationMeasure, Integer durationQuantity,
        LocalDate startDate, LocalTime startTime, String notes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) HospitalizationSummary hospitalization,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EmployeeSummary createdBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        LocalDateTime suspensionDate, EmployeeSummary suspensionBy) {
}
