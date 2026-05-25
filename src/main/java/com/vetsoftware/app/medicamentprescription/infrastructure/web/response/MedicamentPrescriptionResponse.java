package com.vetsoftware.app.medicamentprescription.infrastructure.web.response;

import java.time.LocalDateTime;

public record MedicamentPrescriptionResponse(
        Long id,
        String name,
        String presentation,
        Double quantity,
        String posology,
        PrescriptionSummary prescription,
        LocalDateTime createdDate,
        boolean enabled
) {}
