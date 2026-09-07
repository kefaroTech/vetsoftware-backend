package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Anular ahora dejaría esta aplicación de pago resolviéndose contra un
 * documento que el cliente ya no puede ver. 409.
 */
public class BillingDocumentHasPendingPaymentException extends RuntimeException {

    private final Long billingDocumentId;

    public BillingDocumentHasPendingPaymentException(Long billingDocumentId) {
        super("Este documento de cobro tiene un pago en proceso; espera a que se confirme"
                + " o rechace antes de anularlo.");
        this.billingDocumentId = billingDocumentId;
    }

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }
}
