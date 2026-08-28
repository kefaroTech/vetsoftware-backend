package com.vetsoftware.app.entitlement.domain;

/**
 * El eje se nombra por su codigo del catalogo. El texto se conserva palabra por
 * palabra --incluido el "has exhausted capacity USER" que documenta la
 * incidencia #511-- porque el codigo de {@code limit_dimensions} coincide con
 * el valor que llevaba el enumerado retirado.
 */
public class CompanyCapacityLimitExceededException extends IllegalStateException {
    public CompanyCapacityLimitExceededException(Long companyId, String dimensionCode, int limit,
            int used, int requestedDelta) {
        super("Company " + companyId + " has exhausted capacity " + dimensionCode + ": limit "
                + limit + ", used " + used + ", requested delta " + requestedDelta);
    }
}
