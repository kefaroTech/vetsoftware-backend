package com.vetsoftware.app.companyusageevent.domain;

/**
 * La empresa ya usó el eje hasta su techo vigente (gratuito, de prueba o
 * negociado) y pidió crear uno más.
 *
 * <p>
 * Misma forma que
 * {@code entitlement.domain.CompanyCapacityLimitExceededException} a propósito:
 * los ejes {@code CUMULATIVE}/{@code FLOW} no cuelgan de
 * {@code company_capacities} (no son {@code CAPACITY}), así que no pueden
 * compartir esa clase sin romper el vertical slicing; comparten en cambio el
 * {@code GlobalExceptionHandler}, que traduce las dos al mismo 409 con el mismo
 * cuerpo para que el front no tenga que distinguirlas.
 */
public class CompanyUsageLimitExceededException extends RuntimeException {

    private final Long companyId;
    private final String limitDimensionCode;
    private final int limit;
    private final int used;
    private final int requestedDelta;

    public CompanyUsageLimitExceededException(Long companyId, String limitDimensionCode, int limit,
            int used, int requestedDelta) {
        super("Company " + companyId + " exceeded the limit of dimension " + limitDimensionCode
                + ": limit=" + limit + " used=" + used + " requestedDelta=" + requestedDelta);
        this.companyId = companyId;
        this.limitDimensionCode = limitDimensionCode;
        this.limit = limit;
        this.used = used;
        this.requestedDelta = requestedDelta;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getLimitDimensionCode() {
        return limitDimensionCode;
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
