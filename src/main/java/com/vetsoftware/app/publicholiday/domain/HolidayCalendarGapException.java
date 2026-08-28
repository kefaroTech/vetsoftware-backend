package com.vetsoftware.app.publicholiday.domain;

import java.time.LocalDate;

/**
 * El calculo de un plazo salio del tramo de calendario que se cargo, o entro en
 * un ano del que no hay ni un festivo sembrado.
 *
 * <p>
 * <strong>Esta excepcion existe para no dar nunca una respuesta
 * optimista.</strong> Si un ano no esta sembrado, {@code observed} viene vacio
 * para el, y sin esta guarda el calculo trataria sus dieciocho festivos como
 * dias habiles: el vencimiento saldria <em>mas tarde</em> que el real, que es
 * exactamente la direccion en la que se incumple un plazo legal. Fallar
 * ruidosamente y pedir que se siembre el ano es la unica salida honesta.
 */
public class HolidayCalendarGapException extends RuntimeException {

    public HolidayCalendarGapException(LocalDate date) {
        super("The holiday calendar does not cover " + date
                + ": seed that year before computing business-day deadlines");
    }
}
