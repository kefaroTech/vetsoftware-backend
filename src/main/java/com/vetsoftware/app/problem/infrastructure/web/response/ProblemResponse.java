package com.vetsoftware.app.problem.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.problem.domain.ProblemStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProblemResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        Long animalId, String animalName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ProblemStatus status,
        LocalDate onsetDate, LocalDate resolvedDate, String notes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
