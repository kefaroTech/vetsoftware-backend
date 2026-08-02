package com.vetsoftware.app.prescription.infrastructure.web.response;

public record MedicamentSummary(
    Long id,
    String name,
    String presentation,
    Double quantity,
    String posology,
    String observation) {}
