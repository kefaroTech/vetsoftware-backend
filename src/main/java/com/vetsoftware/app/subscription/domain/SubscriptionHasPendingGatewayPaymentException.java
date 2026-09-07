package com.vetsoftware.app.subscription.domain;

/**
 * El contrato vigente tiene un pago de pasarela {@code PENDING} en vuelo:
 * sustituirlo dejaria esa resolucion apuntando a un contrato que ya no es el
 * vigente de la empresa. 409.
 */
public class SubscriptionHasPendingGatewayPaymentException extends RuntimeException {

    private final Long subscriptionId;

    public SubscriptionHasPendingGatewayPaymentException(Long subscriptionId) {
        super("Todavia hay un cobro en proceso con la pasarela de pago para este contrato;"
                + " espera a que se confirme o rechace antes de continuar.");
        this.subscriptionId = subscriptionId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }
}
