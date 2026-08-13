package com.vetsoftware.app.medicationschedule.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record HospitalizationMedicationSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
