package com.vetsoftware.app.surgery.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateSurgeryRequest(
        @NotNull(message = "La fecha de la cirugía es obligatoria.") LocalDate date,
        @NotNull(message = "Debes seleccionar el tipo de cirugía.") Long surgeryTypeId,
        @NotBlank(message = "La descripción es obligatoria.") @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres.") String description,
        @Size(max = 200, message = "El medicamento no puede superar los 200 caracteres.") String medicament,
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres.") String observations,
        @Size(max = 2000, message = "Las complicaciones no pueden superar los 2000 caracteres.") String complications,
        @NotNull(message = "Debes seleccionar la mascota.") Long animalId, Long consultationId) {
}
