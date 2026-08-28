package com.vetsoftware.app.companytrialgrant.application.command;

import com.vetsoftware.app.companytrialgrant.domain.TrialOutcome;

/**
 * Resolver una prueba.
 *
 * <p>
 * El desenlace puede venir dado —{@code ABANDONED} cuando el cliente quita el
 * módulo antes de vencer— o derivarse de la política congelada en la concesión
 * cuando vence en su fecha. Vacío significa «el que diga su política».
 */
public record ConsumeTrialGrantCommand(Long companyId, Long catalogItemId, TrialOutcome outcome) {
}
