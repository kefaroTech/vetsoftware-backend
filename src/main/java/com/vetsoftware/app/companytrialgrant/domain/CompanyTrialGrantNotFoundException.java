package com.vetsoftware.app.companytrialgrant.domain;

/** No hay concesión de ese artículo para esa empresa. */
public class CompanyTrialGrantNotFoundException extends RuntimeException {

    public CompanyTrialGrantNotFoundException(Long companyId, Long catalogItemId) {
        super("Company " + companyId + " has no trial grant for catalog item " + catalogItemId);
    }
}
