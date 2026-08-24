package com.vetsoftware.app.subscriptionpayment.domain;

/**
 * Transición de estado prohibida por {@link SubscriptionPaymentStatus}. Es un
 * conflicto (409), no un error de entrada: la petición está bien formada y lo
 * que la rechaza es el estado actual de la fila.
 */
public class InvalidSubscriptionPaymentStatusTransitionException extends RuntimeException {
    public InvalidSubscriptionPaymentStatusTransitionException(SubscriptionPaymentStatus from,
            SubscriptionPaymentStatus to) {
        super("Invalid subscription payment status transition: " + from + " -> " + to);
    }
}
