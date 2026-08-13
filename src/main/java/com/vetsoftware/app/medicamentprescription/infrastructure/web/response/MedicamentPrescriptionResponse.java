package com.vetsoftware.app.medicamentprescription.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record MedicamentPrescriptionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long medicamentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String presentation,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Double quantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String posology, String observation,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PrescriptionSummary prescription,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
