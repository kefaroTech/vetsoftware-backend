package com.vetsoftware.app.configurator.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Forma pública de una pregunta con sus opciones. Es la única respuesta del
 * slice que sale sin autenticar, así que su superficie se elige campo a campo y
 * no se deriva de la de administración.
 */
public record QuestionnaireQuestionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String questionText, String helpText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String answerType, Long parentOptionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean required,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<QuestionnaireOptionResponse> options) {
}
