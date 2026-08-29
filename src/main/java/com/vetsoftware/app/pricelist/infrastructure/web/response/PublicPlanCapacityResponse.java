package com.vetsoftware.app.pricelist.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Un contador del plan: cuantas unidades trae y a como sale la siguiente.
 *
 * <p>
 * {@code extraUnitAmount} es el precio del <strong>tramo de entrada</strong>,
 * no la escalera entera: los tramos son acumulativos y publicarlos completos es
 * publicar la politica de descuento por volumen.
 */
public record PublicPlanCapacityResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Codigo del eje: USER, BRANCH...") String unit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int included,
        BigDecimal extraUnitAmount) {
}
