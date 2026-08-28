package com.vetsoftware.app.entitlement.domain;

/**
 * El eje se nombra por su codigo del catalogo. Para los cuatro que ya existian
 * el texto no cambia --el codigo de {@code limit_dimensions} es el mismo
 * {@code USER}, {@code BRANCH}, {@code TERMINAL} o {@code STORAGE_GB} que
 * llevaba el enumerado--, asi que lo que ya leia alguien depurando sigue
 * leyendose igual.
 */
public class CompanyCapacityUnderflowException extends IllegalStateException {
    public CompanyCapacityUnderflowException(Long companyId, String dimensionCode, int used,
            int requestedDelta) {
        super("Company " + companyId + " capacity " + dimensionCode + " cannot go below zero: used "
                + used + ", requested delta " + requestedDelta);
    }
}
