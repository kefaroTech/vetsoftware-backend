package com.vetsoftware.app.paymentgateway.domain;

/** Un pago que ya existe para esta referencia (R13). */
public record PaymentReservation(Long paymentId, String gatewayReference) {
}
