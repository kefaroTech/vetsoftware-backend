package com.vetsoftware.app.configurator.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ConfiguratorOptionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long questionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String label, String helpText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
