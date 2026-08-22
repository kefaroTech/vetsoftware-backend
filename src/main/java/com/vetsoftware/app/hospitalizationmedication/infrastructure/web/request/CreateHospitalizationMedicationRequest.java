package com.vetsoftware.app.hospitalizationmedication.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateHospitalizationMedicationRequest(
        @NotBlank(message = "El nombre del medicamento es obligatorio.") @Size(max = 200, message = "El nombre del medicamento no puede superar los 200 caracteres.") String name,
        @Size(max = 200, message = "La dosis no puede superar los 200 caracteres.") String dose,
        String frequency, String guidelineType, String durationMeasure, Integer durationQuantity,
        LocalDate startDate, LocalTime startTime,
        @Size(max = 2000, message = "Las notas no pueden superar los 2000 caracteres.") String notes,
        @NotNull(message = "Debes seleccionar la hospitalización.") Long hospitalizationId) {
}
