package com.vetsoftware.app.cashregister.domain;

/**
 * Origen del movimiento de caja, para trazar cada movimiento a su documento de
 * origen y para la idempotencia de la orquestación (una venta/abono no se
 * registra dos veces).
 */
public enum CashReferenceType {
    POS_DOCUMENT, OPEN_ACCOUNT_PAYMENT, MANUAL
}
