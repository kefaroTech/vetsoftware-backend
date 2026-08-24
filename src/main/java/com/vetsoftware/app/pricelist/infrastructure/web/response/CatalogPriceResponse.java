package com.vetsoftware.app.pricelist.infrastructure.web.response;

import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CatalogPriceResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long priceListId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long catalogItemId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BillingCycle billingCycle,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int tierMin,
        @Schema(description = "Vacío = del tramo mínimo en adelante") Integer tierMax,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int includedQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Precio unitario sin impuesto") BigDecimal unitAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal setupAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Porcentaje: 19.00 para el 19 %") BigDecimal taxRate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "EXEMPT y EXCLUDED no son lo mismo y no se pueden colapsar en tarifa cero") TaxTreatment taxTreatment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled,
        @Schema(description = "Artículo al que pertenece el precio. Vacío solo si el artículo se retiró del catálogo") CatalogItemSummary catalogItem) {
}
