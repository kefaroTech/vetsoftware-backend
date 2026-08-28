package com.vetsoftware.app.paymentattempt.domain;

public class PaymentAttemptNotFoundException extends RuntimeException {

    public PaymentAttemptNotFoundException(Long id) {
        super("Payment attempt not found: " + id);
    }
}
