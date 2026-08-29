package com.vetsoftware.app.quote.infrastructure.web.response;

import com.vetsoftware.app.quote.domain.TaxTreatment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Un renglon del desglose, tal como aparecera en la oferta.
 *
 * <p>
 * <strong>Un renglon por tramo, no por articulo.</strong> Quince usuarios salen
 * como dos: ocho a 12.000 y cinco a 9.000. Es lo que permite al cliente
 * comprobar la cuenta en vez de creersela.
 *
 * <p>
 * Sin id: la pide un anonimo y se habla por rotulos.
 */
public record QuotePreviewLineResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Lo que se contrato") int contractedQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Lo que la tarifa ya incluye y no se cobra") int includedQuantity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Las unidades que caen en este tramo") int quantity,
        BigDecimal unitAmount, BigDecimal grossAmount, BigDecimal taxRate,
        TaxTreatment taxTreatment, BigDecimal taxAmount, BigDecimal lineTotal) {
}
