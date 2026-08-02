package com.vetsoftware.app.hospitalization.infrastructure.web.request;

import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateHospitalizationRequest(@NotNull LocalDate date, @NotNull LocalDate startDate,
        LocalDate endDate, @NotNull HospitalizationType type, ReasonLeaving reasonLeaving,
        @NotBlank @Size(max = 500) String reason, @Size(max = 2000) String observations,
        @NotNull Long animalId, Long consultationId,
        // Peso opcional al ingreso → se registra en el historial de peso del animal.
        // weightUnit es GRAMS/POUNDS/KILOGRAMS; si es null se usa la unidad preferida
        // del animal.
        @Positive BigDecimal weight, String weightUnit) {
}
