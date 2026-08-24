package com.vetsoftware.app.subscription.domain;

/**
 * Se intento cerrar una linea que ya tenia {@code effective_to}. Cerrarla otra
 * vez reescribiria una fecha que ya es historia.
 *
 * <p>
 * GlobalExceptionHandler: <strong>409</strong>,
 * {@code SUBSCRIPTION_ITEM_ALREADY_ENDED}.
 */
public class SubscriptionItemAlreadyEndedException extends RuntimeException {
    public SubscriptionItemAlreadyEndedException(Long itemId) {
        super("Subscription item already ended: " + itemId);
    }
}
