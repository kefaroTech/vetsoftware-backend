package com.vetsoftware.app.subscription.domain;

import java.time.LocalDate;

/**
 * La vigencia de una linea de contrato, y <strong>el unico sitio de todo el
 * slice donde se escribe que significa «vigente»</strong>.
 *
 * <p>
 * No es «sin fecha de fin» ni «habilitada». Es <em>«ya empezo y todavia no ha
 * terminado»</em>:
 *
 * <pre>
 * effective_from &lt;= dia  AND  (effective_to IS NULL OR effective_to &gt; dia)
 * </pre>
 *
 * El intervalo es <strong>semiabierto</strong> {@code [from, to)}: el dia de
 * fin ya NO esta cubierto. Eso es lo que permite que la linea que cierra el 30
 * y la que abre el 30 no se solapen ni dejen hueco, y es la misma convencion
 * que usa la consulta de vigilancia R7 de
 * {@code docs/db/suscripciones-reglas-codigo.md}.
 *
 * <p>
 * Escribirlo una sola vez no es estetica: cuando este criterio se copia, la
 * copia que se equivoca produce un error <em>invisible</em> —se factura de mas
 * o se dejan permisos vivos— hasta que un cliente reclama meses despues.
 * Cualquier codigo del slice que necesite decidir vigencia o solape llama aqui.
 *
 * @param from
 *            desde cuando cuenta, inclusive. Nunca {@code null}
 * @param to
 *            hasta cuando, exclusive. {@code null} = abierta, todavia vigente
 */
public record EffectivePeriod(LocalDate from, LocalDate to) {

    /**
     * El «para siempre» comparable. Es {@code DATE} y no {@code DATETIME} porque
     * las dos columnas que traduce lo son, y es el mismo literal
     * {@code '9999-12-31'} que usa la consulta de vigilancia.
     */
    public static final LocalDate OPEN_ENDED = LocalDate.of(9999, 12, 31);

    public EffectivePeriod {
        if (from == null)
            throw new IllegalArgumentException("effectiveFrom is required");
        if (to != null && to.isBefore(from))
            throw new IllegalArgumentException("effectiveTo must not be before effectiveFrom");
    }

    /** Vigencia abierta: empieza y no tiene fecha de fin. */
    public static EffectivePeriod openFrom(LocalDate from) {
        return new EffectivePeriod(from, null);
    }

    /** ¿Sigue abierta, es decir sin fecha de fin escrita? */
    public boolean isOpen() {
        return to == null;
    }

    /**
     * El fin comparable: la fecha escrita, o {@link #OPEN_ENDED} si esta abierta.
     * Traduce «vigente» a algo con lo que se puede comparar sin ramificar.
     */
    public LocalDate endExclusive() {
        return to == null ? OPEN_ENDED : to;
    }

    /**
     * <strong>La definicion.</strong> ¿Estaba vigente este dia? Ya empezo
     * ({@code from <= day}) y todavia no termino ({@code endExclusive > day}).
     */
    public boolean isCurrentOn(LocalDate day) {
        if (day == null)
            throw new IllegalArgumentException("day is required");
        return !from.isAfter(day) && endExclusive().isAfter(day);
    }

    /**
     * ¿Se pisan los dos tramos? Criterio de intervalo semiabierto, el mismo de la
     * consulta de vigilancia R7: {@code a.from < b.end AND b.from < a.end}.
     *
     * <p>
     * Existe porque <strong>el esquema garantiza menos de lo que parece</strong>:
     * el indice unico sobre {@code current_item_marker} impide dos lineas
     * <em>abiertas</em> del mismo articulo —el caso comun—, pero dos tramos con
     * fechas de fin futuras que se pisen (A del 1-ene al 30-jun, B del 1-may al
     * 31-dic) dan los dos {@code current_item_marker = NULL} y <strong>MySQL los
     * acepta</strong>: no existen restricciones de exclusion. En mayo y junio ese
     * modulo se factura dos veces, y eso lo tiene que impedir este metodo.
     */
    public boolean overlaps(EffectivePeriod other) {
        if (other == null)
            throw new IllegalArgumentException("other period is required");
        return from.isBefore(other.endExclusive()) && other.from().isBefore(endExclusive());
    }

    /** El mismo tramo cerrado en esa fecha. Dar de baja es esto, nunca borrar. */
    public EffectivePeriod endingOn(LocalDate effectiveTo) {
        return new EffectivePeriod(from, effectiveTo);
    }
}
