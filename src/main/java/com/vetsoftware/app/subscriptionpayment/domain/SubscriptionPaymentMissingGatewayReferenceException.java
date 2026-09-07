package com.vetsoftware.app.subscriptionpayment.domain;

/**
 * Un pago con pasarela ({@code gateway != null}) no puede confirmarse sin
 * {@code gatewayReference}: sin ella, el {@code CONFIRMED} no tiene con que
 * cruzarse contra el extracto de la pasarela y es indistinguible de un ingreso
 * fantasma. 409.
 */
public class SubscriptionPaymentMissingGatewayReferenceException extends RuntimeException {

    private final Long paymentId;

    public SubscriptionPaymentMissingGatewayReferenceException(Long paymentId) {
        super("El pago todavia no tiene una referencia de la pasarela y no puede confirmarse");
        this.paymentId = paymentId;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}
