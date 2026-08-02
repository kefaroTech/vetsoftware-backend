package com.vetsoftware.app.diagnosticimaging.domain;

import java.time.LocalDate;

public record ConsultationRef(Long id, LocalDate date) {
    public ConsultationRef {
        if (id == null)
            throw new IllegalArgumentException("consultation id is required");
        if (date == null)
            throw new IllegalArgumentException("consultation date is required");
    }
}
