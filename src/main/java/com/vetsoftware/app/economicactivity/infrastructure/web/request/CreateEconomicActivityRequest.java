package com.vetsoftware.app.economicactivity.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEconomicActivityRequest(
        @NotBlank(message = "El código de la actividad económica es obligatorio.") @Size(max = 20, message = "El código de la actividad económica no puede superar los 20 caracteres.") String code,
        @NotBlank(message = "El nombre de la actividad económica es obligatorio.") @Size(max = 150, message = "El nombre de la actividad económica no puede superar los 150 caracteres.") String name) {
}
