package com.vetsoftware.app.paymentgateway.domain;

/** Cómo terminó el intento de cobro del primer periodo. */
public enum FirstPeriodChargeOutcome {
    APPROVED, PENDING, DECLINED, NOT_CONFIGURED, NO_PAYMENT_METHOD
}
