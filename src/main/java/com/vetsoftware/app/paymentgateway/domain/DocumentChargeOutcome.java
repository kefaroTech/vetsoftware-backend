package com.vetsoftware.app.paymentgateway.domain;

/**
 * Cómo terminó el intento de cobro de un documento ya emitido. Solo
 * {@link #APPROVED}, {@link #PENDING} y {@link #DECLINED} representan un
 * intento real contra Wompi; el resto son omisiones normales del barrido.
 */
public enum DocumentChargeOutcome {
    APPROVED, PENDING, DECLINED, SKIPPED_NO_BALANCE, SKIPPED_PENDING_PAYMENT, SKIPPED_HARD_DECLINE, SKIPPED_BUDGET, SKIPPED_NOT_DUE, NO_PAYMENT_METHOD, NOT_CONFIGURED
}
