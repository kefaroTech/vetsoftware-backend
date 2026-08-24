package com.vetsoftware.app.subscription.domain;

/**
 * Transicion de estado que el contrato no admite. Incluye el caso «de ACTIVE a
 * ACTIVE», que {@code chk_ssh_change} tambien rechaza: una fila de bitacora que
 * no cambia nada solo ensucia la pelicula.
 *
 * <p>
 * GlobalExceptionHandler: <strong>409</strong>,
 * {@code INVALID_SUBSCRIPTION_STATUS_TRANSITION}.
 */
public class InvalidSubscriptionStatusTransitionException extends RuntimeException {
    public InvalidSubscriptionStatusTransitionException(SubscriptionStatus from,
            SubscriptionStatus to) {
        super("Invalid subscription status transition: " + from + " -> " + to);
    }
}
