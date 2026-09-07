package com.vetsoftware.app.subscriptionpaymentmethod.application.port.out;

public interface PaymentRetryTriggerPort {

    void rescheduleNow(Long companyId, Long attemptId);
}
