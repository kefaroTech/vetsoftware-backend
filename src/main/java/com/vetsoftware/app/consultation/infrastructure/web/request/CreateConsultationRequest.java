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

public record CreateConsultationRequest(
        @NotNull(message = "La fecha de la consulta es obligatoria.") LocalDate date,
        @NotNull(message = "Debes seleccionar el tipo de consulta.") Long consultationTypeId,
        @NotBlank(message = "La anamnesis es obligatoria.") @Size(max = 2000, message = "La anamnesis no puede superar los 2000 caracteres.") String anamnesis,
        @Size(max = 2000, message = "El diagnóstico no puede superar los 2000 caracteres.") String diagnosis,
        @Size(max = 500, message = "El pronóstico no puede superar los 500 caracteres.") String prognosis,
        LocalDate nextControl, @NotNull(message = "Debes seleccionar la mascota.") Long animalId,
        // Peso opcional capturado en la consulta → se registra en el historial de peso
        // del animal.
        // weightUnit es GRAMS/POUNDS/KILOGRAMS; si es null se usa la unidad preferida
        // del animal.
        @Positive(message = "El peso debe ser mayor que cero.") BigDecimal weight,
        String weightUnit,
        // Examen físico / constantes vitales (Fase 3) — todos opcionales.
        @DecimalMin(value = "0", message = "La temperatura no puede ser negativa.") @DecimalMax(value = "60", message = "La temperatura no puede superar los 60 grados.") BigDecimal temperature,
        @Min(value = 0, message = "La frecuencia cardiaca no puede ser negativa.") @Max(value = 1000, message = "La frecuencia cardiaca no puede superar los 1000 latidos por minuto.") Integer heartRate,
        @Min(value = 0, message = "La frecuencia respiratoria no puede ser negativa.") @Max(value = 1000, message = "La frecuencia respiratoria no puede superar las 1000 respiraciones por minuto.") Integer respiratoryRate,
        @Size(max = 40, message = "La descripción de las mucosas no puede superar los 40 caracteres.") String mucousMembranes,
        @Size(max = 20, message = "El tiempo de llenado capilar no puede superar los 20 caracteres.") String capillaryRefill,
        @Size(max = 20, message = "La hidratación no puede superar los 20 caracteres.") String hydration,
        @Min(value = 1, message = "La condición corporal debe ser de al menos 1 punto.") @Max(value = 9, message = "La condición corporal no puede superar los 9 puntos.") Integer bodyConditionScore,
        @Min(value = 0, message = "La escala de dolor no puede ser negativa.") @Max(value = 10, message = "La escala de dolor no puede superar los 10 puntos.") Integer painScore,
        @Size(max = 40, message = "La actitud no puede superar los 40 caracteres.") String attitude,
        @Size(max = 2000, message = "Los hallazgos del examen físico no pueden superar los 2000 caracteres.") String examFindings) {
}
