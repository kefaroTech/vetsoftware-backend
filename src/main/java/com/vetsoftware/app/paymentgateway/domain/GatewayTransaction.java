package com.vetsoftware.app.paymentgateway.domain;

/**
 * Una transacción de Wompi, tal como la devuelve {@code POST /transactions} o
 * {@code GET /transactions/{id}}.
 *
 * @param statusMessage
 *            el motivo crudo del rechazo (p. ej. {@code "insufficient_funds"} o
 *            el texto libre que manda Wompi). No sale por HTTP al tenant: mismo
 *            criterio que {@code PaymentAttempt.getGatewayDeclineCode()}
 */
public record GatewayTransaction(String id, GatewayTransactionStatus status, String statusMessage,
        String reference, long amountInCents, String paymentMethodType) {
}
