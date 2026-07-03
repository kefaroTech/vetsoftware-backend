package com.vetsoftware.app.animalalert.infrastructure.web.request;

import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAnimalAlertRequest(
        @NotNull AlertType type,
        @NotBlank @Size(max = 255) String description,
        AlertSeverity severity
) {}
