package com.vetsoftware.app.promotion.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdatePromotionRequest(
        @NotBlank(message = "El nombre de la promoción es obligatorio.") @Size(max = 100, message = "El nombre de la promoción no puede superar los 100 caracteres.") String name,
        @NotBlank(message = "Debes seleccionar el tipo de promoción.") String promotionType,
        @NotBlank(message = "Debes seleccionar el tipo de aplicación.") String applicationType,
        @NotNull(message = "Debes seleccionar el ítem al que aplica la promoción.") @Positive(message = "El ítem al que aplica la promoción no es válido.") Long applicationItem,
        @NotBlank(message = "Debes seleccionar el tipo de valor.") String valueType,
        @NotNull(message = "El valor de la promoción es obligatorio.") @DecimalMin(value = "0.0", message = "El valor de la promoción no puede ser negativo.") BigDecimal value,
        @NotNull(message = "La fecha de inicio es obligatoria.") LocalDateTime startDate,
        @NotNull(message = "La fecha de fin es obligatoria.") LocalDateTime endDate,
        @NotBlank(message = "Debes seleccionar el estado de la promoción.") String promotionStatus) {
}
