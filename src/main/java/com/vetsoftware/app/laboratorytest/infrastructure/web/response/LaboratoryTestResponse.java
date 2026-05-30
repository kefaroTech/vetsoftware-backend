package com.vetsoftware.app.laboratorytest.infrastructure.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LaboratoryTestResponse(
        Long id,
        LocalDate date,
        LaboratoryTestTypeSummary testType,
        Integer quantity,
        String diagnosis,
        String status,
        String prioridad,
        AnimalSummary animal,
        ConsultationSummary consultation,
        CompanySummary company,
        EmployeeSummary processedBy,
        LocalDateTime processedDate,
        LocalDateTime createdDate,
        boolean enabled
) {}
