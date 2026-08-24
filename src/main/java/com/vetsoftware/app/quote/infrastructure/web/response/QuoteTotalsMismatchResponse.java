package com.vetsoftware.app.quote.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Una fila de la vigilancia de R5: la cabecera frente a la suma de las lineas
 * activas. Lista vacia = sano.
 */
public record QuoteTotalsMismatchResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long quoteId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String quoteNumber,
        @Schema(description = "Vacío en una oferta a prospecto, que todavía no tiene empresa") Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal headerDiscountAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal linesDiscountAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal headerTaxAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal linesTaxAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal headerTotalAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal linesTotalAmount) {
}
