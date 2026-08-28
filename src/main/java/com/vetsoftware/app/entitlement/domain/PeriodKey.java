package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * El periodo al que pertenece un contador, escrito de forma que el propio texto
 * diga de que periodo habla: {@code 2026-03} un mes, {@code 2026-Q3} un
 * trimestre, {@code 2026-S1} un semestre. Siete caracteres exactos, siempre.
 *
 * <p>
 * <strong>Nunca vacio</strong> (R-LIMIT-05). Los cupos que no son de flujo
 * llevan el centinela {@link #SENTINEL}. El motivo es una propiedad del indice
 * unico y no una preferencia de estilo: en
 * {@code uq_company_capacities (company_id, limit_dimension_id, period_key)}
 * dos NULL <em>no chocan entre si</em>, asi que una columna nulable dejaria
 * caber dos contadores para exactamente la misma cosa --y el que sube el
 * consumo actualizaria uno mientras el que lee el techo mira el otro--.
 *
 * <p>
 * El centinela no puede colisionar con ningun periodo real por construccion:
 * todo periodo real empieza por cuatro digitos y {@code ALLTIME} no.
 */
public record PeriodKey(String value) {

    /** Siete caracteres, como todo periodo real. Ver la nota de arriba. */
    public static final String SENTINEL = "ALLTIME";

    // [0-9] y no \d a proposito: el patron viaja por ficheros, scripts y consolas,
    // y una barra invertida de mas o de menos lo convierte en otro patron sin que
    // nada avise. La clase explicita no tiene esa arista.
    private static final Pattern REAL_PERIOD = Pattern
            .compile("^[0-9]{4}-(0[1-9]|1[0-2]|Q[1-4]|S[1-2])$");

    public PeriodKey {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(
                    "period key is required: a non-flow counter carries the sentinel " + SENTINEL
                            + ", never an empty value");
        if (!SENTINEL.equals(value) && !REAL_PERIOD.matcher(value).matches())
            throw new IllegalArgumentException("period key must be " + SENTINEL
                    + " or a real period (YYYY-MM, YYYY-Qn, YYYY-Sn), but was: " + value);
    }

    /** El centinela de los cupos que no son de flujo. */
    public static PeriodKey sentinel() {
        return new PeriodKey(SENTINEL);
    }

    public static PeriodKey of(String value) {
        return new PeriodKey(value);
    }

    /**
     * La clave que le corresponde a un eje segun como se mide.
     *
     * <p>
     * Un eje de flujo <strong>exige</strong> que el llamador diga de que periodo
     * habla, y uno que no lo es <strong>rechaza</strong> que se lo diga. Fallar
     * aqui es lo correcto: inventar un periodo por defecto para un flujo
     * significaria repartir el consumo entre dos filas segun quien lo escribiera.
     */
    public static PeriodKey forMeasure(MeasureKind measureKind, String requested) {
        if (measureKind == null)
            throw new IllegalArgumentException("measure kind is required");
        if (measureKind.requiresPeriodKey()) {
            if (requested == null || requested.isBlank())
                throw new IllegalArgumentException(
                        "a FLOW dimension needs an explicit period key: the caller has to say"
                                + " which period it is consuming");
            return of(requested);
        }
        if (requested != null && !requested.isBlank() && !SENTINEL.equals(requested))
            throw new IllegalArgumentException("a " + measureKind
                    + " dimension does not accept a period key, but received: " + requested);
        return sentinel();
    }

    /**
     * La clave que le corresponde a una linea del contrato ese dia.
     *
     * <p>
     * Es la hermana de {@link #forMeasure(MeasureKind, String)} para el camino del
     * recalculo, donde nadie pide un periodo: el periodo se <em>deriva</em> del dia
     * y de la granularidad que la venta congelo. Hasta aqui el recalculo llamaba a
     * {@code forMeasure(measureKind, null)}, que para un eje de flujo lanza --y
     * estaba bien mientras ninguna linea vendible fuera de flujo, porque fallar en
     * voz alta era mejor que escribir dos contadores mudos--. Ahora que puede
     * serlo, esta es la que sabe responder.
     *
     * <p>
     * Un eje de flujo <strong>exige</strong> granularidad y uno que no lo es la
     * <strong>rechaza</strong>: espejo de
     * {@code chk_catalog_item_limits_reset_period} y de su gemela del contrato, en
     * las dos direcciones.
     */
    public static PeriodKey forContract(MeasureKind measureKind, ResetPeriod resetPeriod,
            LocalDate day) {
        if (measureKind == null)
            throw new IllegalArgumentException("measure kind is required");
        if (!measureKind.requiresPeriodKey()) {
            if (resetPeriod != null)
                throw new IllegalArgumentException("a " + measureKind + " dimension does not reset:"
                        + " it cannot carry a reset period, but received " + resetPeriod);
            return sentinel();
        }
        if (resetPeriod == null)
            throw new IllegalArgumentException("a FLOW dimension needs a reset period to derive its"
                    + " period key: how often the quota starts over is a property of the sale,"
                    + " declared in catalog_item_limits / subscription_item_limits");
        return of(resetPeriod.keyFor(day));
    }

    /** {@code true} si es un periodo real y no el centinela. */
    public boolean isRealPeriod() {
        return !SENTINEL.equals(value);
    }

    /**
     * El primer dia natural del periodo.
     *
     * <p>
     * Existe para el cargo por excedente: un cargo de suscripcion necesita un
     * <em>periodo de servicio</em> con fecha de inicio y de fin, y el unico dato
     * que tiene el contador de cupo es esta clave. Derivarlo aqui —y no en el
     * adaptador que arma el cargo— es lo que impide que cada llamador invente su
     * propio tramo: si dos sitios calcularan el trimestre de forma distinta, el
     * mismo excedente caeria en dos meses contables segun quien lo devengara.
     *
     * @throws IllegalStateException
     *             si la clave es el centinela: un cupo que no es de flujo no tiene
     *             periodo, y devolver una fecha inventada seria escribir un tramo
     *             de servicio que nadie presto
     */
    public LocalDate periodStart() {
        requireRealPeriod();
        int year = year();
        return switch (granularity()) {
            case 'Q' -> LocalDate.of(year, (ordinal() - 1) * 3 + 1, 1);
            case 'S' -> LocalDate.of(year, (ordinal() - 1) * 6 + 1, 1);
            default -> LocalDate.of(year, ordinal(), 1);
        };
    }

    /**
     * El ultimo dia natural del periodo, <strong>inclusive</strong> —que es como lo
     * entiende {@code chk_subscription_charges_period}, cuyo {@code >=} admite el
     * periodo de un solo dia—.
     *
     * @throws IllegalStateException
     *             si la clave es el centinela
     */
    public LocalDate periodEnd() {
        requireRealPeriod();
        LocalDate start = periodStart();
        return switch (granularity()) {
            case 'Q' -> start.plusMonths(3).minusDays(1);
            case 'S' -> start.plusMonths(6).minusDays(1);
            default -> start.withDayOfMonth(start.lengthOfMonth());
        };
    }

    private void requireRealPeriod() {
        if (!isRealPeriod())
            throw new IllegalStateException(
                    "the sentinel " + SENTINEL + " is not a calendar period:"
                            + " a non-flow counter has no service period to derive");
    }

    private int year() {
        return Integer.parseInt(value.substring(0, 4));
    }

    /** {@code 'Q'}, {@code 'S'} o un digito, que es el caso mensual. */
    private char granularity() {
        return value.charAt(5);
    }

    /** El numero de mes, trimestre o semestre. El patron ya garantizo el rango. */
    private int ordinal() {
        char granularity = granularity();
        return granularity == 'Q' || granularity == 'S'
                ? Character.getNumericValue(value.charAt(6))
                : Integer.parseInt(value.substring(5));
    }

    @Override
    public String toString() {
        return value;
    }
}
