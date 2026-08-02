package com.vetsoftware.app.consultationtype.infrastructure.web.response;

import java.time.LocalDateTime;

public record ConsultationTypeResponse(Long id, String name, String description,
        LocalDateTime createdDate, boolean enabled) {
}
