package com.vetsoftware.app.spatype.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSpaTypeRequest(
        @NotBlank(message = "El nombre del tipo de spa es obligatorio.") @Size(max = 100, message = "El nombre del tipo de spa no puede superar los 100 caracteres.") String name,
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.") String description) {
}
