package com.vetsoftware.app.paymentgateway.domain;

public class PaymentSourceRateLimitExceededException extends RuntimeException {

    public PaymentSourceRateLimitExceededException(Long companyId) {
        super("Too many payment source creation attempts for company " + companyId
                + " in the last hour");
    }
}
