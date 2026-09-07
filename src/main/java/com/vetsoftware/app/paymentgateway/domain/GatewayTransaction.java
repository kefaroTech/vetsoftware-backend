package com.vetsoftware.app.paymentgateway.domain;

import java.time.Instant;

/**
 * Una transacción de Wompi, tal como la devuelve {@code POST /transactions},
 * {@code GET /transactions/{id}} o la búsqueda por referencia.
 *
 * @param statusMessage
 *            el motivo crudo del rechazo (p. ej. {@code "insufficient_funds"} o
 *            el texto libre que manda Wompi). No sale por HTTP al tenant: mismo
 *            criterio que {@code PaymentAttempt.getGatewayDeclineCode()}
 * @param createdAt
 *            el instante en que Wompi creó la transacción, o {@code null} si la
 *            respuesta no lo trae.
 */
public record GatewayTransaction(String id, GatewayTransactionStatus status, String statusMessage,
        String reference, long amountInCents, String paymentMethodType, Instant createdAt) {
}
