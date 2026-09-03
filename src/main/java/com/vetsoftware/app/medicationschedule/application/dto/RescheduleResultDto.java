package com.vetsoftware.app.medicationschedule.application.dto;

import com.vetsoftware.app.medicationschedule.domain.CascadeSkipReason;
import java.util.List;

/**
 * Desenlace de una reprogramacion: el plan completo de la medicacion mas si la
 * cascada llego a aplicarse y, si no, por que.
 *
 * <p>
 * Sin los dos ultimos campos, una cascada saltada seria indistinguible de una
 * aplicada sobre un plan de una sola toma.
 */
public record RescheduleResultDto(List<MedicationScheduleDto> schedules, boolean cascadeApplied,
        CascadeSkipReason cascadeSkippedReason) {

    /** La cascada se pidio y se aplico a las tomas siguientes. */
    public static RescheduleResultDto applied(List<MedicationScheduleDto> schedules) {
        return new RescheduleResultDto(schedules, true, null);
    }

    /** No se pidio cascada: no hay nada que reportar mas alla del plan. */
    public static RescheduleResultDto notCascaded(List<MedicationScheduleDto> schedules) {
        return new RescheduleResultDto(schedules, false, null);
    }

    /** Se pidio cascada y no se pudo aplicar; el motivo viaja al cliente. */
    public static RescheduleResultDto skipped(List<MedicationScheduleDto> schedules,
            CascadeSkipReason reason) {
        return new RescheduleResultDto(schedules, false, reason);
    }
}
