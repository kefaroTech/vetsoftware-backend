package com.vetsoftware.app.owner.application.command;

import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;

public record CreateOwnerCommand(
        String name, String email, String document, OwnerDocumentType documentType,
        PersonType personType, String verificationDigit, String legalName, String address,
        String phone, Long cityId, Long companyId, boolean withholdingAgent, TaxRegime taxRegime
) {}
