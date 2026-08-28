package com.vetsoftware.app.paymentrefund.domain;

public class PaymentRefundNotFoundException extends RuntimeException {

    public PaymentRefundNotFoundException(Long id) {
        super("Payment refund not found: " + id);
    }
}
