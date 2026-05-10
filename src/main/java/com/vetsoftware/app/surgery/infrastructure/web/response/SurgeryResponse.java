package com.vetsoftware.app.surgery.infrastructure.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SurgeryResponse(
        Long id,
        LocalDate date,
        SurgeryTypeSummary surgeryType,
        String description,
        String medicament,
        String observations,
        String complications,
        AnimalSummary animal,
        ConsultationSummary consultation,
        CompanySummary company,
        LocalDateTime createdDate
) {}
