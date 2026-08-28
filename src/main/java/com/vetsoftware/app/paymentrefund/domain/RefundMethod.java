package com.vetsoftware.app.paymentrefund.domain;

/**
 * Por donde sale la plata que se devuelve. Dominio cerrado, espejo exacto de
 * {@code chk_payment_refunds_method}: si aqui aparece un valor que la
 * constraint no admite, el {@code INSERT} lo rechaza la base y el fallo llega
 * como un error sin explicacion.
 *
 * <p>
 * <strong>No se llama {@code PaymentMethod} a proposito.</strong> Ese nombre ya
 * lo usa {@code subscriptionpayment.domain.PaymentMethod} y springdoc funde los
 * esquemas por nombre simple: dos enums homonimos en features distintas se
 * publicarian en el contrato como uno solo, con la union de sus valores. Y
 * ademas no son la misma lista — {@code CUSTOMER_CREDIT} solo tiene sentido
 * devolviendo, porque no se puede cobrar con saldo a favor que aun no existe.
 */
public enum RefundMethod {
    CARD, PSE, BANK_TRANSFER, CUSTOMER_CREDIT
}
