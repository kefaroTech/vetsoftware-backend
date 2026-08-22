package com.vetsoftware.app.city.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCityRequest(
        @NotBlank(message = "El nombre de la ciudad es obligatorio.") @Size(max = 100, message = "El nombre de la ciudad no puede superar los 100 caracteres.") String name,
        @NotNull(message = "Debes seleccionar el departamento.") Long stateId,
        @Size(max = 5, message = "El código DANE no puede superar los 5 caracteres.") String daneCode) {
}
