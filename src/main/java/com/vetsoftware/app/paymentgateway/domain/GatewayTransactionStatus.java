package com.vetsoftware.app.paymentgateway.domain;

/**
 * Estado de una transacción en Wompi. Espejo del campo {@code data.status} de
 * la API: una transacción <strong>siempre nace {@link #PENDING}</strong>, y el
 * estado final llega por sondeo ({@code GET /transactions/{id}}) o por webhook.
 */
public enum GatewayTransactionStatus {
    PENDING, APPROVED, DECLINED, VOIDED, ERROR;

    /** Si este estado ya no puede cambiar. */
    public boolean isFinal() {
        return this != PENDING;
    }
}
