package com.vetsoftware.app.clinicalhistory.infrastructure.web.response;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;

public record ClinicalEventResponse(
        Long sourceId,
        ClinicalEventType eventType,
        LocalDate eventDate,
        Long consultationId,
        String summary
) {
}
