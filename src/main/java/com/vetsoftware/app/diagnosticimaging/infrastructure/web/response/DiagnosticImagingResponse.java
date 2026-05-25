package com.vetsoftware.app.diagnosticimaging.infrastructure.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiagnosticImagingResponse(
        Long id,
        LocalDate date,
        DiagnosticImagingTypeSummary diagnosticImagingType,
        String clinicalSigns,
        String studyType,
        String diagnosis,
        String observations,
        String status,
        AnimalSummary animal,
        ConsultationSummary consultation,
        CompanySummary company,
        LocalDateTime createdDate,
        boolean enabled
) {}
