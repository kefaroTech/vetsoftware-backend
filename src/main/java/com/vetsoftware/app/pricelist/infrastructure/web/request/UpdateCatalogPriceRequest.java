package com.vetsoftware.app.pricelist.infrastructure.web.request;

import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Ni la lista ni el artículo viajan: reapuntar un precio a otra lista o a otro
 * artículo no es editarlo, es crear otro.
 */
public record UpdateCatalogPriceRequest(
        @NotNull(message = "El ciclo de facturación es obligatorio.") BillingCycle billingCycle,
        @NotNull(message = "El tramo mínimo es obligatorio.") @Min(value = 1, message = "El tramo mínimo debe ser 1 o mayor.") Integer tierMin,
        @Min(value = 1, message = "El tramo máximo debe ser 1 o mayor.") Integer tierMax,
        @NotNull(message = "La cantidad incluida es obligatoria.") @PositiveOrZero(message = "La cantidad incluida no puede ser negativa.") Integer includedQuantity,
        @NotNull(message = "El precio unitario es obligatorio.") @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo.") BigDecimal unitAmount,
        @NotNull(message = "El cobro de puesta en marcha es obligatorio.") @DecimalMin(value = "0.00", message = "El cobro de puesta en marcha no puede ser negativo.") BigDecimal setupAmount,
        @NotNull(message = "La tarifa de impuesto es obligatoria.") @DecimalMin(value = "0.00", message = "La tarifa de impuesto no puede ser negativa.") @DecimalMax(value = "100.00", message = "La tarifa de impuesto no puede superar 100.") BigDecimal taxRate,
        @NotNull(message = "El tratamiento fiscal es obligatorio.") TaxTreatment taxTreatment) {
}
