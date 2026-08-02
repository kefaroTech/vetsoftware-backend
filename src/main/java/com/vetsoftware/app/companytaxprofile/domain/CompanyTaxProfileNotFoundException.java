package com.vetsoftware.app.companytaxprofile.domain;

public class CompanyTaxProfileNotFoundException extends RuntimeException {
  public CompanyTaxProfileNotFoundException(Long companyId) {
    super("Company tax profile not found for company: " + companyId);
  }
}
