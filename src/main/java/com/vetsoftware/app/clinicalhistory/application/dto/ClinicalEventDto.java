package com.vetsoftware.app.clinicalhistory.application.dto;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;

public record ClinicalEventDto(
        Long sourceId,
        ClinicalEventType eventType,
        LocalDate eventDate,
        Long consultationId,
        String summary
) {
    public static ClinicalEventDto from(ClinicalEvent event) {
        return new ClinicalEventDto(
                event.sourceId(),
                event.eventType(),
                event.eventDate(),
                event.consultationId(),
                event.summary()
        );
    }
}
