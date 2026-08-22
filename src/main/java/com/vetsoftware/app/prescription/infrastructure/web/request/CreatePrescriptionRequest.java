package com.vetsoftware.app.prescription.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreatePrescriptionRequest(
        @NotNull(message = "La fecha de la receta es obligatoria.") LocalDate date,
        // Opcional: se toma el diagnóstico de la consulta a la que pertenece la receta.
        @Size(max = 2000, message = "El diagnóstico no puede superar los 2000 caracteres.") String diagnosis,
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres.") String observations,
        @NotNull(message = "Debes seleccionar la mascota.") Long animalId,
        @NotNull(message = "Debes seleccionar la consulta.") Long consultationId) {
}
