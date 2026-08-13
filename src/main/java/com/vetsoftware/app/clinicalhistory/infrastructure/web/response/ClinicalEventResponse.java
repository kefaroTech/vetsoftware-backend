package com.vetsoftware.app.clinicalhistory.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;

public record ClinicalEventResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sourceId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long animalId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ClinicalEventType eventType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate eventDate, LocalDate endDate,
        Long consultationId, String summary) {
}
