package com.vetsoftware.app.companytaxprofile.infrastructure.web.response;

import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import java.time.LocalDateTime;
import java.util.List;

public record CompanyTaxProfileResponse(
        Long id,
        CompanySummary company,
        CompanyDocumentType companyDocumentType,
        String companyDocumentId,
        String companyDocumentVerificationDigit,
        String legalName,
        TaxRegime taxRegime,
        String fiscalEmail,
        String commercialName,
        EconomicActivitySummary economicActivity,
        List<String> responsibilities,
        LocalDateTime createdDate,
        boolean enabled
) {}
