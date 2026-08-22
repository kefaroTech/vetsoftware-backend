package com.vetsoftware.app.hospitalization.infrastructure.web.request;

import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateHospitalizationRequest(
        @NotNull(message = "La fecha de la hospitalización es obligatoria.") LocalDate date,
        @NotNull(message = "La fecha de ingreso es obligatoria.") LocalDate startDate,
        LocalDate endDate,
        @NotNull(message = "Debes seleccionar el tipo de hospitalización.") HospitalizationType type,
        ReasonLeaving reasonLeaving,
        @NotBlank(message = "El motivo de la hospitalización es obligatorio.") @Size(max = 500, message = "El motivo de la hospitalización no puede superar los 500 caracteres.") String reason,
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres.") String observations,
        @NotNull(message = "Debes seleccionar la mascota.") Long animalId, Long consultationId) {
}
