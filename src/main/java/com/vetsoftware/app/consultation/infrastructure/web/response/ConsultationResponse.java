package com.vetsoftware.app.consultation.infrastructure.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConsultationResponse(
        Long id,
        LocalDate date,
        ConsultationTypeSummary consultationType,
        String anamnesis,
        String diagnosis,
        String therapeuticPlan,
        String diagnosisPlan,
        LocalDate nextControl,
        AnimalSummary animal,
        CompanySummary company,
        LocalDateTime createdDate
) {}
