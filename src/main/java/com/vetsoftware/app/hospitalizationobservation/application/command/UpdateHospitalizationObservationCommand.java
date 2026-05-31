package com.vetsoftware.app.hospitalizationobservation.application.command;

public record UpdateHospitalizationObservationCommand(
        Long id,
        String description
) {}
