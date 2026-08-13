package com.vetsoftware.app.prescription.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record MedicamentSummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, String presentation,
        Double quantity, String posology, String observation) {
}
