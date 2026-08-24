package com.vetsoftware.app.subscriptionpayment.domain;

public class SubscriptionPaymentNotFoundException extends RuntimeException {
    public SubscriptionPaymentNotFoundException(Long id) {
        super("SubscriptionPayment not found: " + id);
    }
}
