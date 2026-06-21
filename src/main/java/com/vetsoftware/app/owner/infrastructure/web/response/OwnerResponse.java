package com.vetsoftware.app.owner.infrastructure.web.response;

import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import java.time.LocalDateTime;

public record OwnerResponse(
        Long id, String name, String email, String document, OwnerDocumentType documentType,
        PersonType personType, String verificationDigit, String legalName, String address,
        String phone, CitySummary city, CompanySummary company, boolean withholdingAgent,
        TaxRegime taxRegime, FiscalResponsibility fiscalResponsibility, LocalDateTime createdDate, boolean enabled
) {}
