package com.vetsoftware.app.paymentgateway.domain;

/**
 * Un documento {@code RECURRING_CYCLE} listo para su primer intento de cobro:
 * con saldo, sin pago pendiente y sin ningún intento todavía.
 */
public record RecurringChargeTarget(Long companyId, Long billingDocumentId) {
}
