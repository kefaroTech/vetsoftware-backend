package com.vetsoftware.app.spa.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateSpaRequest(
        @NotNull(message = "La fecha del servicio de spa es obligatoria.") LocalDate date,
        @NotNull(message = "Debes seleccionar el tipo de spa.") Long spaTypeId,
        @NotBlank(message = "El motivo es obligatorio.") @Size(max = 2000, message = "El motivo no puede superar los 2000 caracteres.") String reason,
        @NotBlank(message = "El detalle es obligatorio.") @Size(max = 2000, message = "El detalle no puede superar los 2000 caracteres.") String details,
        @NotBlank(message = "Las observaciones son obligatorias.") @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres.") String observations,
        @NotNull(message = "Debes seleccionar la mascota.") Long animalId) {
}
