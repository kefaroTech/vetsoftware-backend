package com.vetsoftware.app.animalalert.infrastructure.web.response;

import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import java.time.LocalDateTime;

public record AnimalAlertResponse(
    Long id,
    Long animalId,
    String animalName,
    AlertType type,
    String description,
    AlertSeverity severity,
    LocalDateTime createdDate,
    boolean enabled) {}
