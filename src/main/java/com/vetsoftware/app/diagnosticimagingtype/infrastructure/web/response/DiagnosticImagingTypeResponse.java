package com.vetsoftware.app.diagnosticimagingtype.infrastructure.web.response;

import java.time.LocalDateTime;

public record DiagnosticImagingTypeResponse(
        Long id,
        String name,
        String description,
        CompanySummary company,
        boolean general,
        LocalDateTime createdDate,
        boolean enabled
) {}
