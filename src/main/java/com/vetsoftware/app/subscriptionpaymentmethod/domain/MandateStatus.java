package com.vetsoftware.app.subscriptionpaymentmethod.domain;

/**
 * Estado de la autorizacion para cobrar. Espejo de
 * {@code chk_subscription_payment_methods_mandate_status}.
 *
 * <p>
 * <strong>Los tres estados no valen lo mismo para la cobranza.</strong> Solo
 * {@link #ACTIVE} autoriza a cobrar; {@link #REVOKED} es un derecho que el
 * cliente ejercio y {@link #EXPIRED} es un hecho del calendario. Ninguno de los
 * dos ultimos es un impago, y confundirlos es exactamente el defecto que la
 * tabla existe para evitar.
 */
public enum MandateStatus {

    /** Mandato vivo: es el unico estado que autoriza un cobro. */
    ACTIVE,

    /**
     * El cliente retiro la autorizacion. La ley permite revocar el debito
     * automatico en cualquier momento y sin justificar: <strong>no es una
     * mora</strong>, es el fin del mandato.
     */
    REVOKED,

    /** La tarjeta caduco. Tampoco es un impago: es una fecha que paso. */
    EXPIRED
}
