package com.vetsoftware.app.configurator.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Forma pública de una opción: sin auditoría y sin banderas de administración.
 */
public record QuestionnaireOptionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String label, String helpText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder) {
}
