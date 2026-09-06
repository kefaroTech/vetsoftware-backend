package com.vetsoftware.app.paymentgateway.domain;

/** La fuente de pago que devuelve {@code POST /payment_sources}. */
public record GatewayPaymentSource(Long id, String status) {
}
