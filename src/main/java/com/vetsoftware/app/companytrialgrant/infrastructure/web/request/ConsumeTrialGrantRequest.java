package com.vetsoftware.app.companytrialgrant.infrastructure.web.request;

import com.vetsoftware.app.companytrialgrant.domain.TrialOutcome;

/**
 * Resolver una prueba: escribir cuándo acabó y cómo.
 *
 * <p>
 * <strong>Esto no desconcede nada</strong> (R-TRIAL-22). No hay borrado ni
 * desactivación en toda la rodaja —la tabla ni siquiera lleva {@code enabled}—:
 * lo que se escribe es el <em>desenlace</em>, que es lo que convierte «cuántas
 * de las pruebas de esta campaña acabaron pagando» en una consulta. Quitar el
 * módulo antes de vencer lo marca {@code ABANDONED}, la concesión sigue
 * existiendo y ese artículo sigue sin poder regalarse otra vez.
 *
 * <p>
 * <strong>{@code outcome} vacío es un valor con significado</strong>, no un
 * campo olvidado: significa «el que diga su política congelada», que es el caso
 * normal cuando la prueba vence en su fecha. Se declara explícito para las
 * resoluciones que <em>no</em> se derivan de la política, y la única de hoy es
 * {@code ABANDONED}. Por eso no lleva {@code @NotNull}.
 */
public record ConsumeTrialGrantRequest(TrialOutcome outcome) {
}
