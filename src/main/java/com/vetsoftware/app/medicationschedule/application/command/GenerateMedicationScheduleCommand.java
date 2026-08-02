package com.vetsoftware.app.medicationschedule.application.command;

public record GenerateMedicationScheduleCommand(
    Long hospitalizationMedicationId, Long createdById) {}
