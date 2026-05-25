package com.vetsoftware.app.clinicalhistory.domain;

import java.time.LocalDate;

public record ClinicalEvent(
        Long sourceId,
        Long animalId,
        Long companyId,
        Long consultationId,
        LocalDate eventDate,
        LocalDate endDate,
        ClinicalEventType eventType,
        String summary
) {
    public ClinicalEvent {
        if (sourceId == null) throw new IllegalArgumentException("sourceId is required");
        if (animalId == null) throw new IllegalArgumentException("animalId is required");
        if (companyId == null) throw new IllegalArgumentException("companyId is required");
        if (eventDate == null) throw new IllegalArgumentException("eventDate is required");
        if (eventType == null) throw new IllegalArgumentException("eventType is required");
    }
}
