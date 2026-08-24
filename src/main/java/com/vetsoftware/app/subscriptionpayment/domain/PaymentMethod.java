package com.vetsoftware.app.subscriptionpayment.domain;

/**
 * Cómo entró la plata. Dominio cerrado, espejo exacto de
 * {@code chk_subscription_payments_method}: si aquí aparece un valor que la
 * constraint no admite, el {@code INSERT} lo rechaza la base y el fallo llega
 * como un 409 sin explicación.
 */
public enum PaymentMethod {
    TRANSFER, CARD, PSE, CASH, OTHER
}
