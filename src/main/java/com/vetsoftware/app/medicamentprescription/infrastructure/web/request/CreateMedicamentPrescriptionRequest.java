package com.vetsoftware.app.medicamentprescription.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMedicamentPrescriptionRequest(
        @NotNull Long medicamentId,
        @jakarta.validation.constraints.NotBlank @Size(max = 200) String presentation,
        @NotNull @Positive Double quantity,
        @jakarta.validation.constraints.NotBlank @Size(max = 1000) String posology,
        // Observación por medicamento (opcional).
        @Size(max = 1000) String observation,
        @NotNull Long prescriptionId
) {}
