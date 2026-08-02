package com.vetsoftware.app.animalalert.application.command;

import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;

public record CreateAnimalAlertCommand(Long animalId, AlertType type, String description,
        AlertSeverity severity, Long companyId) {
}
