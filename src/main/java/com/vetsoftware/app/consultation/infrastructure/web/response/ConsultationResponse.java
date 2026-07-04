package com.vetsoftware.app.consultation.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConsultationResponse(
        Long id,
        LocalDate date,
        ConsultationTypeSummary consultationType,
        String anamnesis,
        String diagnosis,
        String prognosis,
        LocalDate nextControl,
        AnimalSummary animal,
        CompanySummary company,
        LocalDateTime createdDate,
        boolean enabled,
        BigDecimal temperature,
        Integer heartRate,
        Integer respiratoryRate,
        String mucousMembranes,
        String capillaryRefill,
        String hydration,
        Integer bodyConditionScore,
        Integer painScore,
        String attitude,
        String examFindings
) {}
