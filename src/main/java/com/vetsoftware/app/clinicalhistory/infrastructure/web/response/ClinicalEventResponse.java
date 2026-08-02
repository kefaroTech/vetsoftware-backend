package com.vetsoftware.app.clinicalhistory.infrastructure.web.response;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;

public record ClinicalEventResponse(Long sourceId, Long animalId, ClinicalEventType eventType,
        LocalDate eventDate, LocalDate endDate, Long consultationId, String summary) {
}
