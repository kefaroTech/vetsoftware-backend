package com.vetsoftware.app.laboratorytestfile.domain;

import java.time.LocalDate;

public record LaboratoryTestRef(Long id, LocalDate date) {
    public LaboratoryTestRef {
        if (id == null) throw new IllegalArgumentException("laboratoryTest id is required");
        if (date == null) throw new IllegalArgumentException("laboratoryTest date is required");
    }
}
