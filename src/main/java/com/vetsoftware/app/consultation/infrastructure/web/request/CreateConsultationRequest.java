package com.vetsoftware.app.consultation.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateConsultationRequest(
        @NotNull LocalDate date,
        @NotNull Long consultationTypeId,
        @NotBlank @Size(max = 2000) String anamnesis,
        @Size(max = 2000) String diagnosis,
        @Size(max = 2000) String therapeuticPlan,
        @Size(max = 2000) String diagnosisPlan,
        LocalDate nextControl,
        @NotNull Long animalId,
        // Peso opcional capturado en la consulta → se registra en el historial de peso del animal.
        // weightUnit es GRAMS/POUNDS/KILOGRAMS; si es null se usa la unidad preferida del animal.
        @Positive BigDecimal weight,
        String weightUnit
) {}
