package com.vetsoftware.app.pricelist.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Un modulo que el plan enciende, y hasta cuando es gratis.
 *
 * <p>
 * {@code trialDays} es opcional a proposito: nulo significa «este modulo no
 * tiene prueba». El front no puede tratarlo como cero ni como treinta.
 */
public record PublicPlanIncludedResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, Integer trialDays) {
}
