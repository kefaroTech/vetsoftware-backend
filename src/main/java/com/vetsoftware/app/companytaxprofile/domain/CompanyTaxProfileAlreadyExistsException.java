package com.vetsoftware.app.companytaxprofile.domain;

public class CompanyTaxProfileAlreadyExistsException extends RuntimeException {
    public CompanyTaxProfileAlreadyExistsException(Long companyId) {
        super("Company tax profile already exists for company: " + companyId);
    }
}
