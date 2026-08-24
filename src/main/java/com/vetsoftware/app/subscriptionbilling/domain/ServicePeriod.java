package com.vetsoftware.app.subscriptionbilling.domain;

import java.time.LocalDate;

/**
 * El tramo de calendario en el que se prestó el servicio.
 *
 * <p>
 * Existe como tipo propio porque <b>devengar, facturar y cobrar son tres cosas
 * distintas</b> y esta es la que fija la primera: sin el periodo de servicio no
 * se puede cerrar un mes contable, porque no hay forma de decidir si un cargo
 * pertenece a agosto o a septiembre.
 *
 * <p>
 * Espejo de {@code chk_subscription_charges_period} y de
 * {@code chk_sbd_period}.
 */
public record ServicePeriod(LocalDate start, LocalDate end) {

    public ServicePeriod {
        if (start == null)
            throw new IllegalArgumentException("service period start is required");
        if (end == null)
            throw new IllegalArgumentException("service period end is required");
        if (end.isBefore(start))
            throw new IllegalArgumentException(
                    "service period end cannot be before start: " + start + " > " + end);
    }

    /**
     * Días naturales del periodo, contando los dos extremos.
     *
     * <p>
     * Es el denominador natural de un prorrateo, pero <b>no</b> se usa para derivar
     * {@link ProrationBasis}: los días del periodo se guardan tal como los calculó
     * quien devengó, porque un contrato puede prorratear sobre 30 días comerciales
     * aunque el mes tenga 31. Reconstruirlo aquí sería inventar un número que no es
     * el que se cobró.
     */
    public int diasNaturales() {
        return (int) (end.toEpochDay() - start.toEpochDay()) + 1;
    }

    /**
     * {@code true} si los dos extremos coinciden con los del otro periodo.
     *
     * <p>
     * <b>Periodo exacto, nunca «el mismo mes».</b> Es el criterio de
     * {@code uq_sbd_recurring_cycle}: agrupar por mes hacía que la factura anual
     * emitida a mitad de agosto chocara con la mensual del día 1 y el cambio a plan
     * anual fuera irregistrable.
     */
    public boolean esElMismoPeriodoExacto(ServicePeriod otro) {
        return otro != null && start.equals(otro.start()) && end.equals(otro.end());
    }
}
