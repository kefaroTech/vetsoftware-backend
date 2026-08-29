package com.vetsoftware.app.configurator.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import java.util.Set;

/**
 * Las respuestas del prospecto.
 *
 * @param selectedOptionIds
 *            ids de las opciones marcadas
 * @param numericAnswers
 *            de cada pregunta numérica, el número respondido
 * @param billingCycle
 *            {@code MONTHLY} o {@code ANNUAL}.
 *            <p>
 *            <strong>Por que el ciclo entra en una peticion que solo
 *            «resuelve».</strong> Resolver ya no devuelve una lista de codigos
 *            neutra: descuenta lo que el contrato trae incluido, y ese techo
 *            sale de {@code catalog_prices.included_quantity}, que es
 *            <em>columna de la fila de precio</em> y tiene una fila por ciclo.
 *            Que hoy los dos ciclos coincidan en la semilla 310 es una
 *            propiedad del dato, no del diseno. Sin este campo, el dia que un
 *            catalogo los separe el configurador restaria el techo mensual de
 *            una cotizacion anual y <strong>nadie se enteraria</strong>: las
 *            dos cifras seguirian siendo plausibles.
 *            <p>
 *            Ademas el resultado ya depende del ciclo por el otro lado: la
 *            pantalla alterna mensual y anual y la cantidad facturable puede
 *            cambiar con el techo.
 *            <p>
 *            <strong>{@code String} y no el enumerado</strong>, por lo mismo
 *            que {@code SelfServeQuoteRequest}: springdoc fusiona esquemas por
 *            nombre simple y hay tres {@code BillingCycle} en el proyecto. El
 *            {@code @Pattern} lo rechaza en el borde —error de campo y no un
 *            500 desde el {@code valueOf}— y el {@code allowableValues} publica
 *            la lista en el contrato, que es de donde los fronts generan su
 *            union en vez de escribirla a mano.
 */
public record ResolveConfiguratorSelectionRequest(Set<Long> selectedOptionIds,
        Map<Long, Integer> numericAnswers,
        @NotBlank @Pattern(regexp = "MONTHLY|ANNUAL") @Schema(allowableValues = {
                "MONTHLY", "ANNUAL"}) String billingCycle) {
}
