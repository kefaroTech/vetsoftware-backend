package com.vetsoftware.app.animalalert.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import java.time.LocalDateTime;

public record AnimalAlertResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        Long animalId, String animalName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AlertType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        AlertSeverity severity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
