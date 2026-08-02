package com.vetsoftware.app.companytaxprofile.infrastructure.web.request;

import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateCompanyTaxProfileRequest(
    @NotNull CompanyDocumentType documentType,
    @NotBlank @Size(max = 20) String companyDocumentId,
    @Size(max = 1) String companyDocumentVerificationDigit,
    @NotBlank @Size(max = 255) String legalName,
    @NotNull TaxRegime taxRegime,
    @NotBlank @Size(max = 255) String fiscalEmail,
    @Size(max = 150) String commercialName,
    Long economicActivityId,
    List<@NotBlank @Size(max = 10) String> responsibilities) {}
