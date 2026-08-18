package com.vetsoftware.app.medicationschedule.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MedicationScheduleResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) HospitalizationMedicationSummary hospitalizationMedication,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime originalDateTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime currentDateTime,
        LocalDateTime realDateTime, String appliedStatus, Boolean rescheduled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EmployeeSummary createdBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
