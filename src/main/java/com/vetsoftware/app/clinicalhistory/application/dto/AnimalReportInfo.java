package com.vetsoftware.app.clinicalhistory.application.dto;

import java.time.LocalDate;

public record AnimalReportInfo(Long id, String name, String code, String specieName,
        String breedName, String colorName, String genderLabel, String reproductiveStateLabel,
        LocalDate birthDate, String ageLabel, String currentWeight, LocalDate weightMeasuredAt,
        boolean deceased, LocalDate deceasedDate, String ownerName, String ownerDocument,
        String ownerPhone, String ownerEmail, String ownerAddress, String companyName,
        String companyIdentifier, String companyAddress, String companyPhone) {
}
