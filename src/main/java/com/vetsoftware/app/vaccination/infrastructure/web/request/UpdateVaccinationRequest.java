package com.vetsoftware.app.vaccination.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateVaccinationRequest(
        @NotNull(message = "La fecha de la vacunación es obligatoria.") LocalDate date,
        @NotNull(message = "Debes seleccionar el tipo de vacuna.") Long vaccinationTypeId,
        @NotBlank(message = "El número de lote es obligatorio.") @Size(max = 100, message = "El número de lote no puede superar los 100 caracteres.") String lot,
        @Size(max = 2000, message = "Las notas no pueden superar los 2000 caracteres.") String notes,
        // Vía de administración y sitio de aplicación (WSAVA) — opcionales.
        @Size(max = 30, message = "La vía de administración no puede superar los 30 caracteres.") String route,
        @Size(max = 60, message = "El sitio de aplicación no puede superar los 60 caracteres.") String applicationSite,
        LocalDate nextVaccination,
        @NotNull(message = "Debes seleccionar la mascota.") Long animalId, Long consultationId) {
}
