package com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * El desenlace de una propagación de mejora.
 *
 * <p>
 * El puerto devuelve un {@code int} pelado y esto lo envuelve en un objeto a
 * propósito: un cuerpo JSON escalar no tiene sitio donde crecer, y el día que
 * la operación quiera decir además <em>a qué contratos</em> llegó, añadir un
 * campo aquí no rompe a nadie mientras que cambiar {@code 7} por {@code {...}}
 * rompe a los dos fronts.
 *
 * <p>
 * <strong>Cero es una respuesta correcta</strong>, no un error: significa que
 * ningún contrato vivo mejoraba con ese techo —bajar el cupo de fábrica no toca
 * a nadie (D-75)—.
 */
public record LimitPropagationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int improvedContracts) {
}
