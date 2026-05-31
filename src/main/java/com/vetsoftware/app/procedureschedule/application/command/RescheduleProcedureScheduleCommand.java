package com.vetsoftware.app.procedureschedule.application.command;

import java.time.LocalDateTime;

/** mode: "one" (solo esta ejecución) | "cascade" (recalcula las siguientes en pauta INTERVALO). */
public record RescheduleProcedureScheduleCommand(
        Long scheduleId,
        LocalDateTime newDateTime,
        String mode
) {}
