package com.vetsoftware.app.configurator.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record ConfiguratorQuestionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String questionText, String helpText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String answerType, Long parentOptionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean required,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Opciones activas de la pregunta. Vacía en las NUMBER, que no admiten ninguna") List<ConfiguratorOptionResponse> options) {
}
