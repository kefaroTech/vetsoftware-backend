package com.vetsoftware.app.service.domain;

/**
 * La empresa ya tiene tantas tarifas activas como su techo vigente y pidió
 * activar (crear o reactivar) una más.
 *
 * <p>
 * Misma forma que
 * {@code entitlement.domain.CompanyCapacityLimitExceededException} y que
 * {@code companyusageevent.domain.CompanyUsageLimitExceededException} a
 * propósito: el eje {@code SERVICE_ITEM} no cuelga de ninguna de esas dos
 * rodajas (es {@code STOCK} sobre {@code services.enabled}, no un
 * {@code CAPACITY} ni una bitácora de hechos), así que necesita su propia clase
 * para no cruzar el vertical slicing; comparten en cambio el
 * {@code GlobalExceptionHandler}, que las traduce todas al mismo 409.
 */
public class ServiceLimitExceededException extends RuntimeException {

    private final Long companyId;
    private final String limitDimensionCode;
    private final int limit;
    private final int used;
    private final int requestedDelta;

    public ServiceLimitExceededException(Long companyId, String limitDimensionCode, int limit,
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
