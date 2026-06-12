package com.vetsoftware.app.owner.infrastructure.web.response;

import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import java.time.LocalDateTime;

public record OwnerResponse(
        Long id, String name, String email, String document, OwnerDocumentType documentType,
        PersonType personType, String verificationDigit, String legalName, String address,
        String phone, CitySummary city, CompanySummary company, LocalDateTime createdDate,
        boolean enabled
) {}
