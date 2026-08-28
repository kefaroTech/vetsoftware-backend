package com.vetsoftware.app.publicholiday.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * El calendario laboral colombiano de un tramo concreto, y la unica pieza del
 * sistema que sabe convertir «quince dias habiles» en una fecha.
 *
 * <p>
 * <strong>De aqui salen todos los plazos del producto</strong>: los quince dias
 * habiles del reclamo de habeas data, los diez de la consulta, el ultimo dia
 * habil de marzo del certificado de retencion, la ventana de retracto. Si el
 * calculo contara dias corridos, el vencimiento saldria <em>mas tarde</em> que
 * el real y el incumplimiento se produciria sin que nada avisara: el error
 * siempre cae del lado de incumplir, nunca del de sobrar. Por eso esto es un
 * objeto de dominio con sus invariantes y no un helper suelto.
 *
 * <p>
 * <strong>Dos guardas, no una.</strong> El tramo cargado ({@code coveredFrom} a
 * {@code coveredTo}) acota hasta donde se puede caminar; {@code coveredYears}
 * dice de que anos hay siembra. Un tramo que cruza a un ano sin sembrar
 * <em>tiene</em> festivos, solo que este objeto no los conoce: tratarlos como
 * habiles seria justo el fallo silencioso que se quiere evitar, asi que se
 * lanza {@link HolidayCalendarGapException}.
 *
 * @param coveredFrom
 *            primer dia del tramo cargado, inclusive
 * @param coveredTo
 *            ultimo dia del tramo cargado, inclusive
 * @param coveredYears
 *            anos con al menos un festivo sembrado
 * @param observedHolidays
 *            fechas <em>observadas</em> —nunca las nominales—
 */
public record HolidayCalendar(LocalDate coveredFrom, LocalDate coveredTo, Set<Integer> coveredYears,
        Set<LocalDate> observedHolidays) {

    public HolidayCalendar {
        if (coveredFrom == null || coveredTo == null) {
            throw new IllegalArgumentException("the covered range is required");
        }
        if (coveredFrom.isAfter(coveredTo)) {
            throw new IllegalArgumentException("coveredFrom cannot be after coveredTo");
        }
        if (coveredYears == null || observedHolidays == null) {
            throw new IllegalArgumentException("coveredYears and observedHolidays are required");
        }
        coveredYears = Collections.unmodifiableSet(new HashSet<>(coveredYears));
        observedHolidays = Collections.unmodifiableSet(new LinkedHashSet<>(observedHolidays));
    }

    /**
     * Un dia es habil si no es sabado, ni domingo, ni festivo <em>observado</em>.
     *
     * @throws HolidayCalendarGapException
     *             si la fecha cae fuera de lo cargado
     */
    public boolean isBusinessDay(LocalDate date) {
        requireCovered(date);
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY
                && !observedHolidays.contains(date);
    }

    /**
     * La fecha en la que vence un plazo de {@code businessDays} dias habiles
     * contado desde {@code start}.
     *
     * <p>
     * <strong>El dia de partida no cuenta.</strong> El plazo empieza a correr el
     * dia habil siguiente al hecho —la radicacion del reclamo, la notificacion—,
     * que es como lo cuenta la norma colombiana; contar el propio dia de partida
     * adelantaria el vencimiento una jornada y es el error inverso, el que da un
     * plazo mas corto del que la ley concede.
     *
     * @throws HolidayCalendarGapException
     *             si el recorrido se sale del tramo cargado o entra en un ano sin
     *             sembrar
     */
    public LocalDate deadline(LocalDate start, int businessDays) {
        if (start == null) {
            throw new IllegalArgumentException("start is required");
        }
        if (businessDays < 1) {
            throw new IllegalArgumentException("businessDays must be at least 1");
        }
        LocalDate cursor = start;
        int counted = 0;
        while (counted < businessDays) {
            cursor = cursor.plusDays(1);
            if (isBusinessDay(cursor)) {
                counted++;
            }
        }
        return cursor;
    }

    /**
     * Cuantos festivos entre semana se saltaron entre {@code start} (excluido) y
     * {@code dueDate} (incluido). No participa en el calculo: es lo que permite a
     * quien lee la respuesta ver <em>por que</em> el vencimiento cayo donde cayo.
     */
    public int weekdayHolidaysBetween(LocalDate start, LocalDate dueDate) {
        if (start == null || dueDate == null || !dueDate.isAfter(start)) {
            return 0;
        }
        int total = 0;
        for (LocalDate cursor = start.plusDays(1); !cursor.isAfter(dueDate); cursor = cursor
                .plusDays(1)) {
            if (observedHolidays.contains(cursor) && cursor.getDayOfWeek() != DayOfWeek.SATURDAY
                    && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                total++;
            }
        }
        return total;
    }

    private void requireCovered(LocalDate date) {
        if (date.isBefore(coveredFrom) || date.isAfter(coveredTo)
                || !coveredYears.contains(date.getYear())) {
            throw new HolidayCalendarGapException(date);
        }
    }
}
