package com.vetsoftware.app.hospitalization.infrastructure.web.response;

import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HospitalizationResponse(
        Long id,
        LocalDate date,
        LocalDate startDate,
        LocalDate endDate,
        HospitalizationType type,
        ReasonLeaving reasonLeaving,
        String reason,
        String observations,
        AnimalSummary animal,
        ConsultationSummary consultation,
        CompanySummary company,
        LocalDateTime createdDate
) {}
