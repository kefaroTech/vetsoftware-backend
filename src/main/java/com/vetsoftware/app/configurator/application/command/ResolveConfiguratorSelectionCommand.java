package com.vetsoftware.app.configurator.application.command;

import java.util.Map;
import java.util.Set;

/**
 * Las respuestas del prospecto, camino de la resolución.
 *
 * <p>
 * Sin {@code companyId} y sin id de cotización: resolver es una función pura
 * sobre el cuestionario. Quien guarda el resultado es {@code quote}, que es
 * también quien sabe de qué empresa —o de qué prospecto sin empresa todavía— es
 * la oferta.
 */
public record ResolveConfiguratorSelectionCommand(Set<Long> selectedOptionIds,
        Map<Long, Integer> numericAnswers) {
}
