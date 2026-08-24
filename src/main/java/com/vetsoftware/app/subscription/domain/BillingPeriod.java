package com.vetsoftware.app.subscription.domain;

import java.time.LocalDate;

/**
 * El periodo de facturacion en curso del contrato, y el <strong>unico sitio del
 * slice donde se cuentan los dias de un prorrateo</strong>.
 *
 * <p>
 * Traduce {@code subscriptions.current_period_start} y
 * {@code current_period_end}, que son un intervalo <strong>cerrado</strong>
 * {@code [start, end]}: los dos extremos se facturan. Ojo con la diferencia
 * frente a {@link EffectivePeriod}, que es semiabierto {@code [from, to)}
 * porque responde otra pregunta —cuando esta vigente una linea— y no esta.
 * Mezclarlas cuesta exactamente un dia de cuota en cada otrosi.
 *
 * @param start
 *            primer dia del periodo, facturado
 * @param end
 *            ultimo dia del periodo, tambien facturado
 */
public record BillingPeriod(LocalDate start, LocalDate end) {

    public BillingPeriod {
        if (start == null)
            throw new IllegalArgumentException("billing period start is required");
        if (end == null)
            throw new IllegalArgumentException("billing period end is required");
        if (end.isBefore(start))
            throw new IllegalArgumentException(
                    "billing period end must not be before start: " + start + " > " + end);
    }

    /**
     * El contrato con su periodo vigente. Vive aqui y no en el caso de uso para que
     * los cuatro otrosies lo lean igual.
     */
    public static BillingPeriod of(Subscription subscription) {
        if (subscription == null)
            throw new IllegalArgumentException("subscription is required");
        return new BillingPeriod(subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd());
    }

    /**
     * El denominador del prorrateo: dias naturales del periodo, contando los dos
     * extremos.
     *
     * <p>
     * <strong>Dias reales, nunca 30 comerciales.</strong> El periodo es el que la
     * fila del contrato dice que es; inventar un mes de 30 dias produciria un
     * importe que no corresponde a ningun tramo de calendario y que nadie podria
     * reconstruir despues.
     */
    public int days() {
        return (int) (end.toEpochDay() - start.toEpochDay()) + 1;
    }

    /**
     * <strong>El numerador.</strong> Los dias de este periodo de facturacion que
     * caen dentro del tramo afectado por el cambio.
     *
     * <p>
     * El tramo llega como {@link EffectivePeriod} —el unico sitio del slice donde
     * se escribe que significa «vigente»— y no como una fecha suelta, y esa
     * decision cierra dos agujeros silenciosos:
     * <ul>
     * <li>Un alta puede traer su propia {@code effectiveFrom}, distinta de la fecha
     * del otrosi. Prorratear por la del otrosi cobraria dias que la linea no sirve.
     * <li>Un alta puede nacer ya con {@code effectiveTo} dentro de este mismo
     * periodo. Sin recortar por el final se cobraria el periodo entero por una
     * linea que dura diez dias.
     * </ul>
     * Para una baja o una cancelacion el tramo es {@link EffectivePeriod#openFrom}
     * desde la fecha efectiva: los dias que dejan de servirse. Como
     * {@code EffectivePeriod} es semiabierto {@code [from, to)}, el dia de la fecha
     * efectiva lo cubre un alta y no lo cubre una baja — de modo que dar de alta y
     * dar de baja el mismo dia suma exactamente cero.
     *
     * <p>
     * Los dos extremos van acotados al periodo a proposito:
     * <ul>
     * <li><strong>Tramo que empieza antes</strong> (otrosi retroactivo) cuenta
     * desde el primer dia de este periodo. Los periodos ya cerrados no se retocan:
     * el dinero es append-only y una correccion hacia atras es otro documento, no
     * este.
     * <li><strong>Tramo que no toca este periodo</strong> cuenta cero: el cambio
     * empieza en un ciclo futuro —o termino en uno pasado— asi que no hay nada que
     * prorratear aqui. Lo que si cambia es la cuota recurrente.
     * </ul>
     */
    public int daysCoveredBy(EffectivePeriod affected) {
        if (affected == null)
            throw new IllegalArgumentException("affected period is required");
        LocalDate first = affected.from().isBefore(start) ? start : affected.from();
        LocalDate endExclusive = affected.endExclusive();
        LocalDate last = endExclusive.isAfter(end) ? end : endExclusive.minusDays(1);
        if (last.isBefore(first))
            return 0;
        return (int) (last.toEpochDay() - first.toEpochDay()) + 1;
    }
}
