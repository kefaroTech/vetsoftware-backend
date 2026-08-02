package com.vetsoftware.app.medicament.infrastructure.web.response;

import java.time.LocalDateTime;

public record MedicamentResponse(Long id, String name, String description, CompanySummary company,
        boolean general, LocalDateTime createdDate, boolean enabled) {
}
