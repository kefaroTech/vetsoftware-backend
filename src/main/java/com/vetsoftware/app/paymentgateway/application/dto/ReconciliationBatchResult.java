package com.vetsoftware.app.paymentgateway.application.dto;

/**
 * El resultado de un lote del barrido de conciliación.
 *
 * @param processed
 *            pagos {@code PENDING} envejecidos examinados
 * @param resolved
 *            de esos, cuántos llegaron a un estado final (Wompi ya había
 *            resuelto la transacción)
 * @param failures
 *            fallos transitorios de la pasarela al consultar; el pago sigue
 *            {@code PENDING} y lo recoge la siguiente pasada
 */
public record ReconciliationBatchResult(int processed, int resolved, int failures) {
}
