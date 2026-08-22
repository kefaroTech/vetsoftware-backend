package com.vetsoftware.app.tax.infrastructure.web.request;

import com.vetsoftware.app.tax.domain.TaxScheme;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateTaxRequest(
        @NotBlank(message = "El nombre del impuesto es obligatorio.") @Size(max = 100, message = "El nombre del impuesto no puede superar los 100 caracteres.") String name,
        @NotNull(message = "El porcentaje es obligatorio.") @DecimalMin(value = "0.0", message = "El porcentaje no puede ser negativo.") @DecimalMax(value = "100.0", message = "El porcentaje no puede superar el 100 %.") BigDecimal percentage,
        @NotNull(message = "Debes seleccionar el esquema de impuesto.") TaxScheme taxScheme,
        @NotNull(message = "No se pudo identificar la versión del impuesto. Vuelve a cargarlo e inténtalo de nuevo.") Long version) {
}
