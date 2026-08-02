package com.vetsoftware.app.clinicalhistory.application.dto;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;

public record ClinicalEventDto(Long sourceId, Long animalId, ClinicalEventType eventType,
        LocalDate eventDate, LocalDate endDate, Long consultationId, String summary) {
    public static ClinicalEventDto from(ClinicalEvent event) {
        return new ClinicalEventDto(event.sourceId(), event.animalId(), event.eventType(),
                event.eventDate(), event.endDate(), event.consultationId(), event.summary());
    }
}
