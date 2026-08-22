package com.vetsoftware.app.animal.infrastructure.web.request;

import com.vetsoftware.app.animal.domain.WeightType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateWeightRecordRequest(
        @NotNull(message = "El peso es obligatorio.") @Positive(message = "El peso debe ser mayor que cero.") BigDecimal value,
        // Opcional: si no viene, se usa la unidad preferida del animal
        // (animal.weightType).
        WeightType unit,
        // Opcional: fecha de la medición; por defecto hoy. No puede ser futura.
        LocalDate measuredAt,
        @Size(max = 500, message = "La observación no puede superar los 500 caracteres.") String note) {
}
