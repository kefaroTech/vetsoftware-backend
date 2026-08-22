package com.vetsoftware.app.state.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStateRequest(
        @NotBlank(message = "El nombre del departamento es obligatorio.") @Size(max = 100, message = "El nombre del departamento no puede superar los 100 caracteres.") String name,
        @NotNull(message = "Debes seleccionar el país.") Long countryId,
        @Size(max = 2, message = "El código DANE no puede superar los 2 caracteres.") String daneCode) {
}
