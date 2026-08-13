package com.vetsoftware.app.companytaxprofile.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import java.time.LocalDateTime;
import java.util.List;

public record CompanyTaxProfileResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanyDocumentType companyDocumentType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String companyDocumentId,
        String companyDocumentVerificationDigit,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String legalName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaxRegime taxRegime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fiscalEmail,
        String commercialName, EconomicActivitySummary economicActivity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> responsibilities,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {
}
