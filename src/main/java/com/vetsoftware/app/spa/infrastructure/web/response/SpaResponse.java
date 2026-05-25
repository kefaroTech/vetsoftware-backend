package com.vetsoftware.app.spa.infrastructure.web.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SpaResponse(
        Long id,
        LocalDate date,
        SpaTypeSummary spaType,
        String reason,
        String details,
        String observations,
        AnimalSummary animal,
        CompanySummary company,
        LocalDateTime createdDate,
        boolean enabled
) {}
