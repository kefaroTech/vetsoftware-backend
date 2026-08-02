package com.vetsoftware.app.animalalert.application.dto;

import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import java.time.LocalDateTime;

public record AnimalAlertDto(Long id, Long animalId, String animalName, AlertType type,
        String description, AlertSeverity severity, LocalDateTime createdDate, boolean enabled) {
    public static AnimalAlertDto from(AnimalAlert alert) {
        return new AnimalAlertDto(alert.getId(), alert.getAnimal().id(), alert.getAnimal().name(),
                alert.getType(), alert.getDescription(), alert.getSeverity(),
                alert.getCreatedDate(), alert.isEnabled());
    }
}
