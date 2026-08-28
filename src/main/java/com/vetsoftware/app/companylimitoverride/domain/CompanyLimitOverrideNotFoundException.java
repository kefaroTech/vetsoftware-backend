package com.vetsoftware.app.companylimitoverride.domain;

/** No hay excepción viva para esa empresa y ese eje. */
public class CompanyLimitOverrideNotFoundException extends RuntimeException {

    public CompanyLimitOverrideNotFoundException(Long companyId, Long limitDimensionId) {
        super("Company " + companyId + " has no live limit override on dimension "
                + limitDimensionId);
    }
}
