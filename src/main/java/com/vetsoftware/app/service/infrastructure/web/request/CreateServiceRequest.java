package com.vetsoftware.app.service.infrastructure.web.request;

import com.vetsoftware.app.service.domain.TaxTreatment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank(message = "El nombre del servicio es obligatorio.") @Size(max = 100, message = "El nombre del servicio no puede superar los 100 caracteres.") String name,
        @NotNull(message = "El precio es obligatorio.") @DecimalMin(value = "0.0", message = "El precio no puede ser negativo.") BigDecimal price,
        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres.") String notes,
        @NotNull(message = "Debes indicar el tratamiento de impuestos.") TaxTreatment taxTreatment,
        @NotNull(message = "Debes seleccionar una categoría de servicio.") Long serviceCategoryId,
        Long taxId) {
}
