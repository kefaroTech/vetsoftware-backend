package com.vetsoftware.app.configurator.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateConfiguratorOptionRequest(
        @NotNull(message = "Debes seleccionar la pregunta.") Long questionId,
        @NotBlank(message = "El código de la opción es obligatorio.") @Size(max = 50, message = "El código de la opción no puede superar los 50 caracteres.") String code,
        @NotBlank(message = "El texto de la opción es obligatorio.") @Size(max = 255, message = "El texto de la opción no puede superar los 255 caracteres.") String label,
        @Size(max = 500, message = "El texto de ayuda no puede superar los 500 caracteres.") String helpText,
        @NotNull(message = "El orden es obligatorio.") @Min(value = 0, message = "El orden no puede ser negativo.") Integer sortOrder) {
}
