package com.vetsoftware.app.medicationschedule.application.command;

import java.time.LocalDateTime;

/** mode: "one" (solo esta toma) | "cascade" (recalcula las siguientes en pauta INTERVALO). */
public record RescheduleMedicationScheduleCommand(
        Long scheduleId,
        LocalDateTime newDateTime,
        String mode
) {}
