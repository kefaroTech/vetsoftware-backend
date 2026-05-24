package com.vetsoftware.app.prescription.application.dto;

import com.vetsoftware.app.prescription.domain.MedicamentRef;
import com.vetsoftware.app.prescription.domain.Prescription;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionDto(
        Long id,
        LocalDate date,
        String diagnosis,
        String observations,
        AnimalSummaryDto animal,
        ConsultationSummaryDto consultation,
        CompanySummaryDto company,
        List<MedicamentRef> medicaments,
        LocalDateTime createdDate
) {
    public static PrescriptionDto from(Prescription prescription) {
        return from(prescription, List.of());
    }

    public static PrescriptionDto from(Prescription prescription, List<MedicamentRef> medicaments) {
        return new PrescriptionDto(
            prescription.getId(),
            prescription.getDate(),
            prescription.getDiagnosis(),
            prescription.getObservations(),
            AnimalSummaryDto.from(prescription.getAnimal()),
            ConsultationSummaryDto.from(prescription.getConsultation()),
            CompanySummaryDto.from(prescription.getCompany()),
            medicaments,
            prescription.getCreatedDate()
        );
    }
}
