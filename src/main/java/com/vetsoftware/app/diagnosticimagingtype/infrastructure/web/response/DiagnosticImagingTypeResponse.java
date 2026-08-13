package com.vetsoftware.app.diagnosticimagingtype.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record DiagnosticImagingTypeResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean general,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        boolean enabled) {
}
