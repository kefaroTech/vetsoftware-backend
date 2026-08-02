package com.vetsoftware.app.laboratorytesttype.infrastructure.web.response;

import java.time.LocalDateTime;

public record LaboratoryTestTypeResponse(Long id, String name, String description,
        CompanySummary company, boolean general, LocalDateTime createdDate, boolean enabled) {
}
