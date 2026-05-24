package com.vetsoftware.app.prescription.domain;

public record MedicamentRef(
        Long id,
        String name,
        String presentation,
        Double quantity,
        String posology
) {
    public MedicamentRef {
        if (id == null) throw new IllegalArgumentException("medicament id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("medicament name is required");
        if (presentation == null || presentation.isBlank())
            throw new IllegalArgumentException("medicament presentation is required");
        if (quantity == null) throw new IllegalArgumentException("medicament quantity is required");
        if (posology == null || posology.isBlank())
            throw new IllegalArgumentException("medicament posology is required");
    }
}
