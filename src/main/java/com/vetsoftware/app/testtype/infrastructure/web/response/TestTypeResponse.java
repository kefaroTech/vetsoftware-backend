package com.vetsoftware.app.testtype.infrastructure.web.response;

import java.time.LocalDateTime;

public record TestTypeResponse(
        Long id,
        String name,
        String description,
        CompanySummary company,
        boolean general,
        LocalDateTime createdDate
) {}
