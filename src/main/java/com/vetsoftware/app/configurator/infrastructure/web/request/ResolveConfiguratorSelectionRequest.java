package com.vetsoftware.app.configurator.infrastructure.web.request;

import java.util.Map;
import java.util.Set;

/**
 * Las respuestas del prospecto.
 *
 * @param selectedOptionIds
 *            ids de las opciones marcadas
 * @param numericAnswers
 *            de cada pregunta numérica, el número respondido
 */
public record ResolveConfiguratorSelectionRequest(Set<Long> selectedOptionIds,
        Map<Long, Integer> numericAnswers) {
}
