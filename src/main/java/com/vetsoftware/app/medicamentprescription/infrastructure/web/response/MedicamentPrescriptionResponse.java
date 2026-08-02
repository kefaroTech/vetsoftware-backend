package com.vetsoftware.app.medicamentprescription.infrastructure.web.response;

import java.time.LocalDateTime;

public record MedicamentPrescriptionResponse(
    Long id,
    Long medicamentId,
    String name,
    String presentation,
    Double quantity,
    String posology,
    String observation,
    PrescriptionSummary prescription,
    LocalDateTime createdDate,
    boolean enabled) {}
