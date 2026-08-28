package com.vetsoftware.app.companyactivitymonth.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Pattern;

/**
 * El mes al que pertenece una fila de actividad, en formato {@code AAAA-MM}.
 *
 * <p>
 * <strong>Siempre mensual, y por eso la columna es {@code CHAR(7)} y no
 * {@code VARCHAR(7)}.</strong> Es la diferencia deliberada con
 * {@code company_usage_events.period_key}, que admite tres granularidades
 * ({@code 2026-03}, {@code 2026-Q3}, {@code ALLTIME}) y ahi el relleno de un
 * {@code CHAR} seria real. Aqui el {@code REGEXP} de {@code chk_cam_period_key}
 * prohibe cualquier cosa que no sean siete caracteres exactos, asi que no hay
 * relleno posible.
 *
 * <p>
 * <strong>La comparacion lexicografica es la cronologica</strong> con este
 * formato y la colacion {@code ascii_bin} de la columna: {@code '2026-02'} va
 * antes que {@code '2026-10'} sin necesidad de convertir a fecha. De eso
 * depende que el orden de un listado por periodo sea el orden del calendario.
 *
 * <p>
 * Es un {@code record} sin identidad ni estado: se valida al construirse y no
 * hay forma de tener uno mal formado dentro del dominio.
 */
public record ActivityPeriodKey(String value) {

    /**
     * Espejo literal de {@code chk_cam_period_key}. Es el mismo
     * {@code ^[0-9]{4}-(0[1-9]|1[0-2])$} que {@code accounting_periods} usa para
     * {@code period_key}: mes de dos digitos entre {@code 01} y {@code 12}, nunca
     * {@code 2026-1} ni {@code 2026-13}.
     */
    private static final Pattern MONTHLY = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");

    public ActivityPeriodKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("periodKey is required");
        }
        if (!MONTHLY.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "periodKey must be a month in AAAA-MM format, between 01 and 12: " + value);
        }
    }

    /** La clave del mes al que pertenece una fecha. */
    public static ActivityPeriodKey of(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        return of(YearMonth.from(date));
    }

    /** La clave de un mes concreto. */
    public static ActivityPeriodKey of(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("month is required");
        }
        return new ActivityPeriodKey(
                String.format("%04d-%02d", month.getYear(), month.getMonthValue()));
    }

    /**
     * Cuantos dias tiene este mes.
     *
     * <p>
     * Es el techo real de {@code active_days} y el motivo de que
     * {@code chk_cam_active_days} sea {@code BETWEEN 0 AND 31} y no
     * {@code <= lengthOfMonth()}: un {@code CHECK} de MySQL mira una fila y podria
     * escribirlo, pero {@code 31} es el techo que vale para los doce meses sin
     * depender de la funcion de calendario. La comprobacion fina —28 dias activos
     * en febrero de un ano no bisiesto es imposible— vive aqui, donde si se puede
     * mirar el mes concreto.
     */
    public int lengthOfMonth() {
        return YearMonth.of(Integer.parseInt(value.substring(0, 4)),
                Integer.parseInt(value.substring(5, 7))).lengthOfMonth();
    }

    @Override
    public String toString() {
        return value;
    }
}
