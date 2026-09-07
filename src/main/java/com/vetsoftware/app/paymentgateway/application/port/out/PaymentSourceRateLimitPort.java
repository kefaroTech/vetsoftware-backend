package com.vetsoftware.app.paymentgateway.application.port.out;

public interface PaymentSourceRateLimitPort {

    void checkAndConsume(Long companyId);
}
