package com.vetsoftware.app.companylimitoverride.domain;

import java.time.LocalDateTime;

/**
 * Esa excepción ya está revocada. Revocarla otra vez movería la fecha en que el
 * cliente dejó de tener el techo pactado, que es el dato con el que se defiende
 * la decisión seis meses después.
 */
public class OverrideAlreadyRevokedException extends RuntimeException {

    public OverrideAlreadyRevokedException(Long companyId, Long limitDimensionId,
            LocalDateTime revokedAt) {
        super("The limit override of company " + companyId + " on dimension " + limitDimensionId
                + " was already revoked at " + revokedAt);
    }
}
