package com.vetsoftware.app.deworming.infrastructure.web.request;

import com.vetsoftware.app.deworming.domain.DewormingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateDewormingRequest(
        @NotNull(message = "La fecha de la desparasitación es obligatoria.") LocalDate date,
        LocalDate lastDeworming,
        @NotNull(message = "Debes seleccionar el tipo de desparasitación.") DewormingType type,
        @NotBlank(message = "El producto es obligatorio.") @Size(max = 200, message = "El producto no puede superar los 200 caracteres.") String product,
        @NotBlank(message = "La dosis es obligatoria.") @Size(max = 200, message = "La dosis no puede superar los 200 caracteres.") String dosage,
        LocalDate nextControl,
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres.") String observations,
        @NotNull(message = "Debes seleccionar la mascota.") Long animalId, Long consultationId) {
}
