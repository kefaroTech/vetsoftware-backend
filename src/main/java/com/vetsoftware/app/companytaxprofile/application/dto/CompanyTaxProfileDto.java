package com.vetsoftware.app.companytaxprofile.application.dto;

import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import java.time.LocalDateTime;
import java.util.List;

public record CompanyTaxProfileDto(Long id, CompanySummaryDto company,
        CompanyDocumentType companyDocumentType, String companyDocumentId,
        String companyDocumentVerificationDigit, String legalName, TaxRegime taxRegime,
        String fiscalEmail, String commercialName, EconomicActivitySummaryDto economicActivity,
        List<String> responsibilities, LocalDateTime createdDate, boolean enabled) {
    public static CompanyTaxProfileDto from(CompanyTaxProfile profile) {
        return new CompanyTaxProfileDto(profile.getId(),
                profile.getCompany() == null ? null : CompanySummaryDto.from(profile.getCompany()),
                profile.getCompanyDocumentType(), profile.getCompanyDocumentId(),
                profile.getCompanyDocumentVerificationDigit(), profile.getLegalName(),
                profile.getTaxRegime(), profile.getFiscalEmail(), profile.getCommercialName(),
                profile.getEconomicActivity() == null
                        ? null
                        : EconomicActivitySummaryDto.from(profile.getEconomicActivity()),
                profile.getResponsibilities().stream().map(CompanyTaxProfileResponsibility::code)
                        .toList(),
                profile.getCreatedDate(), profile.isEnabled());
    }
}
