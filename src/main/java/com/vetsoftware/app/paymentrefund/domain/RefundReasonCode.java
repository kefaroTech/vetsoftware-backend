package com.vetsoftware.app.paymentrefund.domain;

/**
 * Por que se devuelve, en lista cerrada. Espejo de
 * {@code chk_payment_refunds_reason_code}.
 *
 * <p>
 * Va junto a {@code reason}, que es texto libre, y la pareja es deliberada: es
 * la convencion de la casa para los motivos. El codigo permite <em>agrupar</em>
 * —cuantas devoluciones fueron por error de facturacion este trimestre— y el
 * texto permite <em>explicar</em> el caso concreto. Con solo el texto no hay
 * informe posible; con solo el codigo no hay expediente.
 */
public enum RefundReasonCode {
    WITHDRAWAL, BILLING_ERROR, CANCELLATION_CREDIT, REVERSAL, DUPLICATE_PAYMENT, OTHER
}
