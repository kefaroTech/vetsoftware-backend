package com.vetsoftware.app.daycare.infrastructure.web.response;

import com.vetsoftware.app.daycare.domain.DayCareType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DayCareResponse(
        Long id,
        LocalDate date,
        LocalDate startDate,
        LocalDate endDate,
        DayCareType type,
        String objects,
        String observations,
        AnimalSummary animal,
        CompanySummary company,
        LocalDateTime createdDate
) {}
