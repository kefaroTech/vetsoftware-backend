package com.vetsoftware.app.entitlement.domain;

public class CompanyCapacityLimitExceededException extends IllegalStateException {
    public CompanyCapacityLimitExceededException(Long companyId, CapacityUnit unit, int limit,
            int used, int requestedDelta) {
        super("Company " + companyId + " has exhausted capacity " + unit.name() + ": limit " + limit
                + ", used " + used + ", requested delta " + requestedDelta);
    }
}
