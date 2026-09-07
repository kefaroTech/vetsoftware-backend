package com.vetsoftware.app.entitlement.domain;

/**
 * El eje se nombra por su codigo del catalogo. El texto se conserva palabra por
 * palabra --incluido el "has exhausted capacity USER" que documenta la
 * incidencia #511-- porque el codigo de {@code limit_dimensions} coincide con
 * el valor que llevaba el enumerado retirado.
 */
public class CompanyCapacityLimitExceededException extends IllegalStateException {

    private final Long companyId;
    private final String dimensionCode;
    private final int limit;
    private final int used;
    private final int requestedDelta;

    public CompanyCapacityLimitExceededException(Long companyId, String dimensionCode, int limit,
            int used, int requestedDelta) {
        super("Company " + companyId + " has exhausted capacity " + dimensionCode + ": limit "
                + limit + ", used " + used + ", requested delta " + requestedDelta);
        this.companyId = companyId;
        this.dimensionCode = dimensionCode;
        this.limit = limit;
        this.used = used;
        this.requestedDelta = requestedDelta;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getDimensionCode() {
        return dimensionCode;
    }

    public int getLimit() {
        return limit;
    }

    public int getUsed() {
        return used;
    }

    public int getRequestedDelta() {
        return requestedDelta;
    }
}
