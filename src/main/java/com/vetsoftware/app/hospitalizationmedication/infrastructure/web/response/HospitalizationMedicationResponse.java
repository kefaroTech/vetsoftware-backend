package com.vetsoftware.app.hospitalizationmedication.infrastructure.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record HospitalizationMedicationResponse(
        Long id,
        String name,
        String dose,
        String frequency,
        String guidelineType,
        String durationMeasure,
        Integer durationQuantity,
        LocalDate startDate,
        LocalTime startTime,
        String notes,
        HospitalizationSummary hospitalization,
        EmployeeSummary createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {}
