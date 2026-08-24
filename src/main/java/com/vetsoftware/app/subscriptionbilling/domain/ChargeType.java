package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Qué clase de devengo representa un cargo.
 *
 * <p>
 * Espejo de {@code chk_subscription_charges_type}, y la mitad de
 * {@code chk_subscription_charges_sign}: el tipo decide qué signos admite el
 * {@code subtotal_amount} del cargo.
 */
public enum ChargeType {
    /** La cuota del ciclo. Siempre suma: {@code subtotal_amount >= 0}. */
    RECURRING,
    /**
     * El proporcional de un cambio a mitad de ciclo. <b>Libre de signo a
     * propósito</b>: una ampliación cobra (positivo) y una reducción acredita
     * (negativo), y las dos son operaciones normales.
     */
    PRORATION,
    /** Implantación, migración, capacitación. Siempre suma. */
    ONE_TIME,
    /** Lo que se le devuelve al cliente. Siempre resta. */
    CREDIT,
    /** Descuento comercial. Siempre resta. */
    DISCOUNT;

    /** {@code true} si el tipo obliga a un subtotal positivo o cero. */
    public boolean exigeSubtotalNoNegativo() {
        return this == RECURRING || this == ONE_TIME;
    }

    /** {@code true} si el tipo obliga a un subtotal negativo o cero. */
    public boolean exigeSubtotalNoPositivo() {
        return this == CREDIT || this == DISCOUNT;
    }
}
