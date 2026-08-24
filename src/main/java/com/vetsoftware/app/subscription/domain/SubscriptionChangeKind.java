package com.vetsoftware.app.subscription.domain;

/**
 * Que cambio del contrato se acaba de consolidar. Es lo que este slice publica
 * hacia fuera para que el recalculo de permisos y contadores (R11, slice
 * {@code entitlement}) sepa que tiene que correr.
 *
 * <p>
 * Este slice <strong>no recalcula nada</strong>: solo dice que el contrato
 * cambio. Quien escucha decide.
 */
public enum SubscriptionChangeKind {
    SUBSCRIPTION_CREATED, ITEM_ADDED, ITEM_REMOVED, QUANTITY_CHANGED, STATUS_CHANGED, CANCELLATION_REQUESTED, EFFECTIVE_DATE_REACHED
}
