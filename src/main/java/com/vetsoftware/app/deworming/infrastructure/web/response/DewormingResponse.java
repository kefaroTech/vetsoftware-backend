package com.vetsoftware.app.deworming.infrastructure.web.response;

import com.vetsoftware.app.deworming.domain.DewormingType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DewormingResponse(
        Long id,
        LocalDate date,
        LocalDate lastDeworming,
        DewormingType type,
        String product,
        String dosage,
        LocalDate nextControl,
        String observations,
        AnimalSummary animal,
        ConsultationSummary consultation,
        CompanySummary company,
        LocalDateTime createdDate,
        boolean enabled
) {}
