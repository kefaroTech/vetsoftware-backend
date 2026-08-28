package com.vetsoftware.app.companyusageevent.domain;

import java.util.regex.Pattern;

/**
 * La clave del periodo contra el que se acumula un hecho de uso.
 *
 * <p>
 * Espejo literal de {@code chk_cue_period_key} (changeset 354). Cuatro formas y
 * solo cuatro: mensual ({@code 2026-03}), trimestral ({@code 2026-Q3}),
 * semestral ({@code 2026-S1}) y el centinela {@code ALLTIME} para los ejes que
 * no se reinician.
 *
 * <p>
 * <strong>La columna es {@code VARCHAR(7)} y no {@code CHAR(7)}, al reves que
 * {@code posting_period}.</strong> Es la misma desviacion deliberada que el
 * changeset 314 documenta para {@code company_capacities}: MySQL recorta los
 * espacios finales de un {@code CHAR} al leerlo, y esta clave admite formas
 * heterogeneas —no solo el {@code AAAA-MM} de un periodo contable— de modo que
 * el dia que entre una forma mas corta el relleno seria real y silencioso.
 * {@code posting_period} puede permitirse {@code CHAR(7)} porque su
 * {@code REGEXP} prohibe cualquier cosa que no sean siete caracteres exactos.
 *
 * <p>
 * <em>Matiz honesto, por si alguien va a apoyarse en esto:</em> las cuatro
 * formas que hoy acepta el {@code CHECK} miden exactamente siete caracteres,
 * asi que <b>hoy la diferencia entre {@code CHAR} y {@code VARCHAR} no cambia
 * nada</b> —el comentario del changeset 354 la justifica con «tres longitudes
 * de contenido distintas», y eso, medido, no es cierto—. La eleccion sigue
 * siendo la buena porque cuesta cero y protege del caso futuro; lo que no hay
 * que hacer es repetir la justificacion sin medirla.
 *
 * <p>
 * <strong>Por que es un objeto de valor y no un {@code String} suelto.</strong>
 * La clave entra desde fuera —la escribe el proceso de medicion— y viaja hasta
 * una columna con {@code CHECK}. Sin este tipo, una clave mal formada se
 * descubre como violacion de integridad en mitad de un lote nocturno, con el
 * lote ya a medias; con el, se rechaza en el borde y el mensaje dice que forma
 * se esperaba.
 */
public record UsagePeriodKey(String value) {

    /**
     * El centinela de los ejes que no se reinician nunca.
     *
     * <p>
     * Se declara con nombre —aunque el {@code REGEXP} de abajo tambien lo lleve
     * literal— porque es el valor que un proceso de medicion tiene que escribir
     * para un eje acumulativo, y buscarlo dentro de una expresion regular es como
     * se acaba escribiendo {@code "ALL_TIME"} y descubriendolo en el {@code CHECK}.
     */
    public static final String ALLTIME = "ALLTIME";

    /**
     * Literalmente el {@code REGEXP} de {@code chk_cue_period_key}. Se escribe
     * igual a proposito: dos expresiones equivalentes pero distintas es como se
     * empieza a aceptar en Java lo que el motor rechaza, o al reves.
     */
    private static final Pattern SHAPE = Pattern
            .compile("^([0-9]{4}-(0[1-9]|1[0-2])|[0-9]{4}-Q[1-4]|[0-9]{4}-S[12]|ALLTIME)$");

    public UsagePeriodKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("periodKey is required");
        }
        if (!SHAPE.matcher(value).matches()) {
            throw new IllegalArgumentException("periodKey '" + value + "' has an unknown shape:"
                    + " the four accepted forms are YYYY-MM (monthly), YYYY-Qn (quarterly),"
                    + " YYYY-Sn (half-yearly) and the ALLTIME sentinel for axes that never reset");
        }
    }

    public static UsagePeriodKey of(String value) {
        return new UsagePeriodKey(value);
    }

}
