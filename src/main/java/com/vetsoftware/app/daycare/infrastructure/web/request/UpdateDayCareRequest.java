package com.vetsoftware.app.daycare.infrastructure.web.request;

import com.vetsoftware.app.daycare.domain.DayCareType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateDayCareRequest(
        @NotNull(message = "La fecha del registro es obligatoria.") LocalDate date,
        @NotNull(message = "La fecha de inicio es obligatoria.") LocalDate startDate,
        LocalDate endDate,
        @NotNull(message = "Debes seleccionar el tipo de guarderia.") DayCareType type,
        @Size(max = 1000, message = "Los objetos no pueden superar los 1000 caracteres.") String objects,
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres.") String observations,
        @NotNull(message = "Debes seleccionar la mascota.") Long animalId) {
}
