package com.vetsoftware.app.configurator.infrastructure.web.request;

import com.vetsoftware.app.configurator.domain.AnswerType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sin {@code code}: la referencia estable de una pregunta no se reescribe. */
public record UpdateConfiguratorQuestionRequest(
        @NotBlank(message = "El texto de la pregunta es obligatorio.") @Size(max = 255, message = "El texto de la pregunta no puede superar los 255 caracteres.") String questionText,
        @Size(max = 500, message = "El texto de ayuda no puede superar los 500 caracteres.") String helpText,
        @NotNull(message = "Debes seleccionar el tipo de respuesta.") AnswerType answerType,
        Long parentOptionId,
        @NotNull(message = "Debes indicar si la pregunta es obligatoria.") Boolean required,
        @NotNull(message = "El orden es obligatorio.") @Min(value = 0, message = "El orden no puede ser negativo.") Integer sortOrder) {
}
