package com.vetsoftware.app.breed.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBreedRequest(
        @NotBlank(message = "El nombre de la raza es obligatorio.") @Size(max = 100, message = "El nombre de la raza no puede superar los 100 caracteres.") String name,
        @NotNull(message = "Debes seleccionar la especie.") Long specieId) {
}
