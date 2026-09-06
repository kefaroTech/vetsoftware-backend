package com.vetsoftware.app.subscription.domain;

/**
 * De donde nace el contrato. Se deriva de {@code quoteId}, no es una columna:
 * {@link Subscription#origin()} es la unica fuente de verdad.
 */
public enum SubscriptionOrigin {

    /** Nacio del alta de la empresa, con el minimo estructural y sin cotizacion. */
    INITIAL,
    /** Nacio de reemplazar el contrato con una cotizacion aceptada. */
    QUOTE
}
