package com.vetsoftware.app.consultation.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateConsultationRequest(@NotNull LocalDate date, @NotNull Long consultationTypeId,
        @NotBlank @Size(max = 2000) String anamnesis, @Size(max = 2000) String diagnosis,
        @Size(max = 500) String prognosis, LocalDate nextControl, @NotNull Long animalId,
        // Peso opcional capturado en la consulta → se registra en el historial de peso
        // del animal.
        // weightUnit es GRAMS/POUNDS/KILOGRAMS; si es null se usa la unidad preferida
        // del animal.
        @Positive BigDecimal weight, String weightUnit,
        // Examen físico / constantes vitales (Fase 3) — todos opcionales.
        @DecimalMin("0") @DecimalMax("60") BigDecimal temperature,
        @Min(0) @Max(1000) Integer heartRate, @Min(0) @Max(1000) Integer respiratoryRate,
        @Size(max = 40) String mucousMembranes, @Size(max = 20) String capillaryRefill,
        @Size(max = 20) String hydration, @Min(1) @Max(9) Integer bodyConditionScore,
        @Min(0) @Max(10) Integer painScore, @Size(max = 40) String attitude,
        @Size(max = 2000) String examFindings) {
}
