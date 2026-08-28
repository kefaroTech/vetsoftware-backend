package com.vetsoftware.app.customercredit.domain;

/**
 * Clase de asiento del libro del saldo a favor. Dominio cerrado, espejo exacto
 * de {@code chk_cce_entry_kind}: si aqui aparece un valor que la constraint no
 * admite, el {@code INSERT} lo rechaza la base y el fallo llega como un error
 * de integridad sin explicacion.
 *
 * <p>
 * El signo del importe no es libre: lo fija {@code chk_cce_sign} y lo repite el
 * constructor de {@link CustomerCreditEntry}. Un {@code GRANT} suma, un
 * {@code CONSUMPTION} y una {@code EXPIRATION} restan, y {@code VOID} y
 * {@code CORRECTION} pueden ir en cualquier sentido porque existen justamente
 * para compensar un asiento anterior.
 */
public enum CreditEntryKind {

    /** Alta de saldo. Es el unico asiento que abre un lote y puede caducar. */
    GRANT,

    /** Consumo contra un lote concreto. Siempre negativo. */
    CONSUMPTION,

    /** Caducidad del remanente de un lote. Siempre negativo. */
    EXPIRATION,

    /** Anulacion de un asiento anterior. */
    VOID,

    /** Correccion manual. */
    CORRECTION;

    /** Los asientos que restan de un lote y por eso lo tienen que nombrar. */
    public boolean consumesLot() {
        return this == CONSUMPTION || this == EXPIRATION;
    }
}
