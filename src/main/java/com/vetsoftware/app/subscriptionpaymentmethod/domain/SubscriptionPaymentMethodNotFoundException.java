package com.vetsoftware.app.subscriptionpaymentmethod.domain;

/** El medio de pago no existe, o no es de la empresa que pregunta (404). */
public class SubscriptionPaymentMethodNotFoundException extends RuntimeException {

    public SubscriptionPaymentMethodNotFoundException(Long id) {
        super("Subscription payment method not found: " + id);
    }
}
