package com.vetsoftware.app.medicament.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMedicamentRequest(
        @NotBlank(message = "El nombre del medicamento es obligatorio.") @Size(max = 200, message = "El nombre del medicamento no puede superar los 200 caracteres.") String name,
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.") String description) {
}
