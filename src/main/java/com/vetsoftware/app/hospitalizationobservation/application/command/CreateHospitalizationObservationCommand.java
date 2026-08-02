package com.vetsoftware.app.hospitalizationobservation.application.command;

public record CreateHospitalizationObservationCommand(String description, Long hospitalizationId,
        Long createdById) {
}
