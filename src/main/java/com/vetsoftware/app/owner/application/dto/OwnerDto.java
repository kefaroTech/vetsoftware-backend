package com.vetsoftware.app.owner.application.dto;

import com.vetsoftware.app.owner.domain.Owner;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import java.time.LocalDateTime;

public record OwnerDto(
        Long id, String name, String email, String document, OwnerDocumentType documentType,
        PersonType personType, String verificationDigit, String legalName, String address,
        String phone, CitySummaryDto city, CompanySummaryDto company, boolean withholdingAgent,
        TaxRegime taxRegime, LocalDateTime createdDate, boolean enabled
) {
    public static OwnerDto from(Owner owner) {
        return new OwnerDto(
            owner.getId(), owner.getName(), owner.getEmail(), owner.getDocument(),
            owner.getDocumentType(), owner.getPersonType(), owner.getVerificationDigit(),
            owner.getLegalName(), owner.getAddress(), owner.getPhone(),
            CitySummaryDto.from(owner.getCity()),
            CompanySummaryDto.from(owner.getCompany()),
            owner.isWithholdingAgent(), owner.getTaxRegime(), owner.getCreatedDate(), owner.isEnabled()
        );
    }
}
