package com.vetsoftware.app.prescription.infrastructure.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PrescriptionResponse(
        Long id,
        LocalDate date,
        String diagnosis,
        String observations,
        AnimalSummary animal,
        ConsultationSummary consultation,
        CompanySummary company,
        LocalDateTime createdDate
) {}
