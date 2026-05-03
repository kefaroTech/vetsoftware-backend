package com.vetsoftware.app.prescription.application.dto;

import com.vetsoftware.app.prescription.domain.Prescription;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PrescriptionDto(
        Long id,
        LocalDate date,
        String diagnosis,
        String observations,
        AnimalSummaryDto animal,
        CompanySummaryDto company,
        LocalDateTime createdDate
) {
    public static PrescriptionDto from(Prescription prescription) {
        return new PrescriptionDto(
            prescription.getId(),
            prescription.getDate(),
            prescription.getDiagnosis(),
            prescription.getObservations(),
            AnimalSummaryDto.from(prescription.getAnimal()),
            CompanySummaryDto.from(prescription.getCompany()),
            prescription.getCreatedDate()
        );
    }
}
