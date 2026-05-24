package com.vetsoftware.app.clinicalhistory.application.dto;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ClinicalHistoryReportModel(
        AnimalReportInfo animal,
        LocalDate from,
        LocalDate to,
        List<ClinicalEventType> typeFilters,
        List<ClinicalEventDto> events,
        LocalDateTime generatedAt
) {
}
