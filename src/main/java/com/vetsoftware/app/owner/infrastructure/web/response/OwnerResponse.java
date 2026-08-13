package com.vetsoftware.app.owner.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import java.time.LocalDateTime;

public record OwnerResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String document,
        OwnerDocumentType documentType, PersonType personType, String verificationDigit,
        String legalName, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String address,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String phone,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CitySummary city,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CompanySummary company,
        boolean withholdingAgent, TaxRegime taxRegime, FiscalResponsibility fiscalResponsibility,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        boolean enabled) {
}
