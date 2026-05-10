package com.vetsoftware.app.medicamentprescription.application.dto;

import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import java.time.LocalDateTime;

public record MedicamentPrescriptionDto(
        Long id,
        String name,
        String presentation,
        Double quantity,
        String posology,
        PrescriptionSummaryDto prescription,
        LocalDateTime createdDate
) {
    public static MedicamentPrescriptionDto from(MedicamentPrescription medicament) {
        return new MedicamentPrescriptionDto(
            medicament.getId(),
            medicament.getName(),
            medicament.getPresentation(),
            medicament.getQuantity(),
            medicament.getPosology(),
            PrescriptionSummaryDto.from(medicament.getPrescription()),
            medicament.getCreatedDate()
        );
    }
}
