package com.vetsoftware.app.companytrialgrant.domain;

import java.time.LocalDateTime;

/**
 * La prueba ya se resolvió. Resolverla otra vez movería la fecha con la que se
 * calcula la tasa de conversión por módulo, que es la cifra que decide qué
 * duración de campaña se repite.
 */
public class TrialAlreadyConsumedException extends RuntimeException {

    public TrialAlreadyConsumedException(Long companyId, Long catalogItemId,
            LocalDateTime consumedAt) {
        super("Trial of catalog item " + catalogItemId + " for company " + companyId
                + " was already resolved at " + consumedAt);
    }
}
