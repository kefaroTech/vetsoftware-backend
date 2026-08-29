package com.vetsoftware.app.configurator.application.command;

import java.util.Map;
import java.util.Set;

/**
 * Las respuestas del prospecto, camino de la resolución.
 *
 * <p>
 * Sin {@code companyId} y sin id de cotización: resolver no necesita saber de
 * quién es la oferta. Quien la guarda es {@code quote}, que es también quien
 * sabe de qué empresa —o de qué prospecto sin empresa todavía— se trata.
 *
 * <p>
 * <strong>Con {@code billingCycle}, y ya no es una función pura del
 * cuestionario.</strong> Dejó de serlo cuando la resolución empezó a descontar
 * lo que el contrato trae incluido: ese techo vive en la fila de precio y hay
 * una por ciclo. El ciclo llega como texto porque el borde REST lo valida con
 * un {@code @Pattern}; el enumerado lo resuelve el caso de uso, que es donde un
 * valor imposible tiene que morir.
 */
public record ResolveConfiguratorSelectionCommand(Set<Long> selectedOptionIds,
        Map<Long, Integer> numericAnswers, String billingCycle) {
}
