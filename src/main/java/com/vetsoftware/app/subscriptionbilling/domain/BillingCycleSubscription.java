package com.vetsoftware.app.subscriptionbilling.domain;

import java.time.LocalDate;

/**
 * El contrato visto <b>desde el reloj de cobro</b>: lo justo para decidir qué
 * periodo toca cobrar y cuándo toca el siguiente.
 *
 * <p>
 * <b>Companion VO</b>, hermano de {@link SubscriptionRef} y con más campos que
 * él a propósito: aquel resuelve una referencia, este alimenta un cálculo de
 * calendario.
 *
 * <p>
 * <b>Aquí no hay ningún campo de estado del contrato, y es deliberado</b>
 * (R-TRIAL-13). Si alguien lo añade, la siguiente línea que se escriba será un
 * {@code if} que filtre por él, y con eso se deja de cobrar la línea de pago
 * obligatorio de quien está en prueba.
 */
public record BillingCycleSubscription(Long id, Long companyId, BillingPeriodicity periodicity,
        LocalDate startDate, LocalDate trialEndDate, LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd, LocalDate nextBillingDate) {

    public BillingCycleSubscription {
        if (id == null)
            throw new IllegalArgumentException("subscription id is required");
        if (companyId == null)
            throw new IllegalArgumentException("subscription companyId is required");
        if (periodicity == null)
            throw new IllegalArgumentException("billing periodicity is required");
        if (startDate == null)
            throw new IllegalArgumentException("startDate is required");
        if (currentPeriodStart == null || currentPeriodEnd == null)
            throw new IllegalArgumentException("current period is required");
    }

    /**
     * <b>El primer día que devenga, que es el día siguiente al fin de la prueba y
     * no el de la firma.</b>
     *
     * <p>
     * Quien firma el 31 de enero con treinta días de prueba no devenga nada hasta
     * que la prueba vence: su primer periodo <b>completo</b> arranca el 2 de marzo,
     * y <b>no hay prorrateo de entrada</b> porque no hay nada que prorratear —el
     * mes de prueba no se cobra, no se cobra a medias—. Anclar a la firma habría
     * generado un cargo fraccionado por unos días que el contrato regalaba.
     */
    public LocalDate firstBillableStart() {
        return trialEndDate == null ? startDate : trialEndDate.plusDays(1);
    }

    /**
     * El ancla del contrato, derivada <b>solo de campos que nunca se
     * reescriben</b>.
     *
     * <p>
     * Ese es el detalle que impide que se degrade: {@code current_period_start} sí
     * lo mueve este mismo proceso, así que derivar el ancla de él la haría avanzar
     * del 31 al 28 y quedarse ahí. {@code start_date} y {@code trial_end_date} son
     * historia y no cambian, de modo que el ancla que se calcula hoy es la misma
     * que se calculará dentro de tres años.
     */
    public BillingAnchor anchor() {
        return BillingAnchor.from(firstBillableStart());
    }

    /**
     * El inicio del periodo que toca cobrar.
     *
     * <p>
     * Es {@code next_billing_date} cuando el contrato ya tiene uno, y el primer día
     * devengable cuando todavía está en prueba o nunca se le facturó. Un
     * {@code next_billing_date} que se hubiera quedado corto —28 de febrero— no
     * corrompe nada: la ventana lo cierra contra el ancla y el contrato vuelve solo
     * a su día.
     */
    public LocalDate periodToBillStart() {
        LocalDate first = firstBillableStart();
        if (nextBillingDate == null || nextBillingDate.isBefore(first))
            return first;
        return nextBillingDate;
    }

    /** La ventana que toca cobrar, cerrada contra el ancla. */
    public BillingCycleWindow windowToBill() {
        return BillingCycleWindow.startingOn(periodToBillStart(), anchor(), periodicity);
    }

    /**
     * ¿Toca cobrarle ya en este barrido?
     *
     * <p>
     * Se cobra <b>por anticipado</b>, el mismo día en que empieza el periodo: el
     * barrido del día D emite el periodo que arranca en D. Esperar a que termine
     * sería cobrar un mes vencido, que no es lo que dice el contrato.
     */
    public boolean dueOn(LocalDate runDate) {
        return !periodToBillStart().isAfter(runDate);
    }
}
