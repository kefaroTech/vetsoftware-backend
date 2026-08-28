package com.vetsoftware.app.publicholiday.application.dto;

import java.time.LocalDate;

/**
 * El resultado de un plazo en dias habiles.
 *
 * <p>
 * {@code weekdayHolidaysSkipped} no participa en el calculo y esta a proposito:
 * un vencimiento que cae tres dias mas alla de lo que uno esperaba es
 * indistinguible de un error de calculo hasta que se ve cuantos festivos entre
 * semana se cruzaron por el camino.
 */
public record BusinessDayDeadlineDto(LocalDate startDate, int businessDays, LocalDate dueDate,
        int weekdayHolidaysSkipped) {
}
