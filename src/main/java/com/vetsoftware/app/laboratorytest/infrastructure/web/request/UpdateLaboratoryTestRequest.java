package com.vetsoftware.app.laboratorytest.infrastructure.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UpdateLaboratoryTestRequest(
        @NotNull(message = "La fecha del examen es obligatoria.") LocalDate date,
        @NotNull(message = "Debes seleccionar el tipo de examen.") Long testTypeId,
        @NotNull(message = "La cantidad es obligatoria.") @Min(value = 1, message = "La cantidad debe ser de al menos 1 unidad.") Integer quantity,
        @Size(max = 2000, message = "El diagnóstico no puede superar los 2000 caracteres.") String diagnosis,
        String prioridad, @NotNull(message = "Debes seleccionar la mascota.") Long animalId,
        Long consultationId, Long processedById, LocalDateTime processedDate) {
}
