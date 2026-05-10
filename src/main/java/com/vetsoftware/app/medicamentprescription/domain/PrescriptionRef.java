package com.vetsoftware.app.medicamentprescription.domain;

import java.time.LocalDate;

public record PrescriptionRef(Long id, LocalDate date) {
    public PrescriptionRef {
        if (id == null) throw new IllegalArgumentException("prescription id is required");
        if (date == null) throw new IllegalArgumentException("prescription date is required");
    }
}
