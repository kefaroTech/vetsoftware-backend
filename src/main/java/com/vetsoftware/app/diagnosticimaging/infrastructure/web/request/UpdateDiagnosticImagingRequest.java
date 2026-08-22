package com.vetsoftware.app.diagnosticimaging.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateDiagnosticImagingRequest(
        @NotNull(message = "La fecha del estudio es obligatoria.") LocalDate date,
        @NotNull(message = "Debes seleccionar el tipo de imagen diagnóstica.") Long diagnosticImagingTypeId,
        @NotBlank(message = "Los signos clínicos son obligatorios.") @Size(max = 2000, message = "Los signos clínicos no pueden superar los 2000 caracteres.") String clinicalSigns,
        @NotBlank(message = "El tipo de estudio es obligatorio.") @Size(max = 200, message = "El tipo de estudio no puede superar los 200 caracteres.") String studyType,
        @NotBlank(message = "El diagnóstico es obligatorio.") @Size(max = 2000, message = "El diagnóstico no puede superar los 2000 caracteres.") String diagnosis,
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres.") String observations,
        @NotNull(message = "Debes seleccionar la mascota.") Long animalId, Long consultationId) {
}
