package com.vetsoftware.app.medicamentprescription.application.dto;

import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import java.time.LocalDateTime;

public record MedicamentPrescriptionDto(
        Long id,
        Long medicamentId,
        String name,
        String presentation,
        Double quantity,
        String posology,
        String observation,
        PrescriptionSummaryDto prescription,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static MedicamentPrescriptionDto from(MedicamentPrescription medicament) {
        return new MedicamentPrescriptionDto(
            medicament.getId(),
            medicament.getMedicamentId(),
            medicament.getName(),
            medicament.getPresentation(),
            medicament.getQuantity(),
            medicament.getPosology(),
            medicament.getObservation(),
            PrescriptionSummaryDto.from(medicament.getPrescription()),
            medicament.getCreatedDate(),
            medicament.isEnabled()
        );
    }
}
