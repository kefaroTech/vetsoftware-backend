package com.vetsoftware.app.specie.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSpecieRequest(
        @NotBlank(message = "El nombre de la especie es obligatorio.") @Size(max = 100, message = "El nombre de la especie no puede superar los 100 caracteres.") String name) {
}
