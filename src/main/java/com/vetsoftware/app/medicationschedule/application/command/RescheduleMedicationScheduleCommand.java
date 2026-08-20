package com.vetsoftware.app.medicationschedule.application.command;

import com.vetsoftware.app.medicationschedule.domain.RescheduleMode;
import java.time.LocalDateTime;

/**
 * {@code mode}: {@link RescheduleMode#ONE} mueve solo esta toma;
 * {@link RescheduleMode#CASCADE} recalcula ademas las siguientes pendientes
 * cuando la pauta es de INTERVALO.
 */
public record RescheduleMedicationScheduleCommand(Long scheduleId, LocalDateTime newDateTime,
        RescheduleMode mode, Long companyId) {
}
