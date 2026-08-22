package com.vetsoftware.app.animalcolor.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAnimalColorRequest(
        @NotBlank(message = "El nombre del color es obligatorio.") @Size(max = 100, message = "El nombre del color no puede superar los 100 caracteres.") String name,
        @NotNull(message = "Debes seleccionar la especie.") Long specieId) {
}
