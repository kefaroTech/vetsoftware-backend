package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Por qué se emitió este documento de cobro.
 *
 * <p>
 * Es la columna <b>[AÑADIDA]</b> por la especificación, y no es un adorno: sin
 * ella la barandilla contra la doble facturación
 * ({@code recurring_cycle_marker}) o cubre todos los {@code INVOICE} —y
 * entonces una factura de prorrateo emitida con el mismo periodo exacto que la
 * de ciclo se rechaza, bloqueando un cobro legítimo— o no cubre nada.
 *
 * <p>
 * Espejo de {@code chk_sbd_billing_reason}.
 */
public enum BillingReason {
    /**
     * La factura del ciclo. <b>La única que entra en la barandilla</b>
     * {@code uq_sbd_recurring_cycle}.
     */
    RECURRING_CYCLE,
    /** Cobro proporcional de un cambio a mitad de ciclo. */
    PRORATION,
    /** Cobro puntual: implantación, migración. */
    ONE_TIME,
    /** Ajuste: nota crédito o débito de corrección. */
    ADJUSTMENT
}
