package com.vetsoftware.app.vaccinationtype.infrastructure.web.response;

import java.time.LocalDateTime;

public record VaccinationTypeResponse(
        Long id,
        String name,
        String description,
        CompanySummary company,
        boolean general,
        LocalDateTime createdDate
) {}
