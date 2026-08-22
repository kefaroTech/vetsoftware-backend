package com.vetsoftware.app.medicamentprescription.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMedicamentPrescriptionRequest(
        @NotNull(message = "Debes seleccionar el medicamento.") Long medicamentId,
        @jakarta.validation.constraints.NotBlank(message = "La presentación es obligatoria.") @Size(max = 200, message = "La presentación no puede superar los 200 caracteres.") String presentation,
        @NotNull(message = "La cantidad es obligatoria.") @Positive(message = "La cantidad debe ser mayor que cero.") Double quantity,
        @jakarta.validation.constraints.NotBlank(message = "La posología es obligatoria.") @Size(max = 1000, message = "La posología no puede superar los 1000 caracteres.") String posology,
        // Observación por medicamento (opcional).
        @Size(max = 1000, message = "La observación no puede superar los 1000 caracteres.") String observation,
        @NotNull(message = "Debes seleccionar la receta.") Long prescriptionId) {
}
