package com.vetsoftware.app.paymentgateway.domain;

/**
 * El resultado de reservar un pago con una llave de idempotencia (R13).
 *
 * @param recovered
 *            {@code true} cuando esta llamada perdió la carrera de inserción y
 *            {@code paymentId} es el pago de la ganadora: quien lo recibe no
 *            debe volver a cargar la tarjeta contra la pasarela.
 */
public record PaymentReservationOutcome(Long paymentId, boolean recovered) {
}
