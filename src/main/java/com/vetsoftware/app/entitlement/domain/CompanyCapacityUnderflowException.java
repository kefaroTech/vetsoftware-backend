package com.vetsoftware.app.entitlement.domain;

public class CompanyCapacityUnderflowException extends IllegalStateException {
    public CompanyCapacityUnderflowException(Long companyId, CapacityUnit unit, int used,
            int requestedDelta) {
        super("Company " + companyId + " capacity " + unit.name() + " cannot go below zero: used "
                + used + ", requested delta " + requestedDelta);
    }
}
