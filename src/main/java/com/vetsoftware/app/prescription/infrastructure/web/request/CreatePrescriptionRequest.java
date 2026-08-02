package com.vetsoftware.app.prescription.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreatePrescriptionRequest(
    @NotNull LocalDate date,
    // Opcional: se toma el diagnóstico de la consulta a la que pertenece la receta.
    @Size(max = 2000) String diagnosis,
    @Size(max = 2000) String observations,
    @NotNull Long animalId,
    @NotNull Long consultationId) {}
