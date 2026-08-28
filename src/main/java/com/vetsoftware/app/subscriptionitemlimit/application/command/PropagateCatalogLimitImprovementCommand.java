package com.vetsoftware.app.subscriptionitemlimit.application.command;

import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;

/**
 * Propagar a los contratos vivos una <em>mejora</em> del techo de fábrica.
 *
 * <p>
 * Es una operación propia, y esa separación es la decisión (D-75). Documentar
 * una mejora que no negoció nadie con la tabla de excepciones negociadas la
 * llenaría de filas sin llamada detrás y vaciaría de significado el informe de
 * a quién se le han hecho excepciones — que es exactamente para lo que existe.
 */
public record PropagateCatalogLimitImprovementCommand(Long catalogItemId, Long limitDimensionId,
        LimitMode factoryMode, Integer factoryLimitQuantity) {
}
