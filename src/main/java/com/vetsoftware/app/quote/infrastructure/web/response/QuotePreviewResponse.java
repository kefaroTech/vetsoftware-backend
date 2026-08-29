package com.vetsoftware.app.quote.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * Cuanto cuesta una seleccion, calculado por el servidor.
 *
 * <p>
 * <strong>Esto es lo que el front debe pintar, y no una cuenta suya.</strong>
 * La escalera de tramos es acumulativa y no se publica —es la politica de
 * descuento por volumen—, asi que un cliente que solo ve el tramo de entrada
 * solo puede extrapolar: quince usuarios le salen 156.000 y la contratacion
 * cobra 141.000. Los importes de aqui los produce el mismo codigo que congela
 * una oferta real, asi que lo que se muestra y lo que se cobra coinciden por
 * construccion.
 *
 * <p>
 * No es una oferta: no tiene numero, ni vigencia, ni estado, y no persiste
 * nada.
 */
public record QuotePreviewResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "ISO 4217 de la tarifa vigente") String currency,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
                "MONTHLY", "ANNUAL"}) String billingCycle,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Un renglon por tramo: el mismo desglose con el que se facturara") List<QuotePreviewLineResponse> lines,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal subtotalAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal discountAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal taxAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal totalAmount) {
}
