package com.vetsoftware.app.clinicalhistory.application.dto;

public record AnimalReportInfo(
        Long id,
        String name,
        String code,
        String specieName,
        String breedName,
        String ownerName,
        String ownerPhone,
        String companyName,
        String companyIdentifier
) {
}
