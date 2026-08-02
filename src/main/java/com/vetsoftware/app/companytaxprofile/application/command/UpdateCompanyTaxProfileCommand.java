package com.vetsoftware.app.companytaxprofile.application.command;

import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import java.util.List;

public record UpdateCompanyTaxProfileCommand(
    CompanyDocumentType companyDocumentType,
    String companyDocumentId,
    String companyDocumentVerificationDigit,
    String legalName,
    TaxRegime taxRegime,
    String fiscalEmail,
    String commercialName,
    Long economicActivityId,
    List<String> responsibilityCodes,
    Long companyId) {}
