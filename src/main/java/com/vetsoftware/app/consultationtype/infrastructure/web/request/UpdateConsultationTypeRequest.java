package com.vetsoftware.app.consultationtype.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateConsultationTypeRequest(
        @NotBlank(message = "El nombre del tipo de consulta es obligatorio.") @Size(max = 100, message = "El nombre del tipo de consulta no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "La descripción es obligatoria.") @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.") String description) {
}
