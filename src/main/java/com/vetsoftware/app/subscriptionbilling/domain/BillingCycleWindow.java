package com.vetsoftware.app.subscriptionbilling.domain;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * El periodo que toca cobrar y la fecha en que tocará el siguiente.
 *
 * <p>
 * Las dos salen del mismo cálculo a propósito: la fecha del próximo cobro
 * <b>es</b> el día siguiente al final de este periodo, y calcularlas por
 * separado es como se llega a un contrato cuyo {@code next_billing_date} no
 * empalma con su {@code current_period_end} — un día de servicio que no se
 * cobra nunca, o que se cobra dos veces.
 */
public record BillingCycleWindow(ServicePeriod period, LocalDate nextBillingDate) {

    public BillingCycleWindow {
        if (period == null)
            throw new IllegalArgumentException("period is required");
        if (nextBillingDate == null)
            throw new IllegalArgumentException("nextBillingDate is required");
        if (!nextBillingDate.equals(period.end().plusDays(1)))
            throw new IllegalArgumentException("nextBillingDate must be the day after the period"
                    + " ends: " + period.end() + " -> " + nextBillingDate);
    }

    /**
     * La ventana que arranca en {@code periodStart}, cerrada <b>contra el ancla</b>
     * y no contra el propio {@code periodStart}.
     *
     * <p>
     * Esa distinción es toda la regla: con el ancla en 31 y un periodo que empezó
     * el 28 de febrero —porque febrero no tiene 31—, el cierre pregunta al ancla
     * por marzo y devuelve el 31, así que el contrato <b>vuelve</b> a su día.
     * Cerrar sumando un mes a {@code periodStart} lo habría dejado en el 28 para
     * siempre.
     */
    public static BillingCycleWindow startingOn(LocalDate periodStart, BillingAnchor anchor,
            BillingPeriodicity periodicity) {
        if (periodStart == null)
            throw new IllegalArgumentException("periodStart is required");
        if (anchor == null)
            throw new IllegalArgumentException("anchor is required");
        if (periodicity == null)
            throw new IllegalArgumentException("periodicity is required");
        LocalDate next = anchor
                .onMonth(YearMonth.from(periodStart).plusMonths(periodicity.months()));
        // Red de seguridad para el arranque desalineado: si el contrato empieza a
        // devengar despues de su propio dia de ancla dentro del mes -una alta el 5 con
        // ancla en el 2-, el ancla del mes siguiente caeria ANTES del inicio y el
        // periodo naceria invertido. Se avanza un mes mas en vez de construir un
        // ServicePeriod imposible.
        while (!next.isAfter(periodStart)) {
            next = anchor.onMonth(YearMonth.from(next).plusMonths(periodicity.months()));
        }
        return new BillingCycleWindow(new ServicePeriod(periodStart, next.minusDays(1)), next);
    }
}
