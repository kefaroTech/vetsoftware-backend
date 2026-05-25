package com.vetsoftware.app.surgerytype.infrastructure.web.response;

import java.time.LocalDateTime;

public record SurgeryTypeResponse(
        Long id,
        String name,
        String description,
        CompanySummary company,
        boolean general,
        LocalDateTime createdDate,
        boolean enabled
) {}
