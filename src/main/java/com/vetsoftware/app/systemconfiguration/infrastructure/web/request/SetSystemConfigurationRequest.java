package com.vetsoftware.app.systemconfiguration.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetSystemConfigurationRequest(
        @NotBlank(message = "El nombre de la propiedad es obligatorio.") @Size(max = 100, message = "El nombre de la propiedad no puede superar los 100 caracteres.") String propertyName,
        @NotBlank(message = "El valor es obligatorio.") @Size(max = 255, message = "El valor no puede superar los 255 caracteres.") String value) {
}
