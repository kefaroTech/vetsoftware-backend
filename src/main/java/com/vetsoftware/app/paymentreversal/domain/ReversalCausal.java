package com.vetsoftware.app.paymentreversal.domain;

/**
 * Las <strong>cinco causales tasadas</strong> de la reversion de pago, y
 * ninguna mas.
 *
 * <p>
 * La lista es <strong>legal y cerrada</strong>: no es un catalogo que el
 * negocio pueda ampliar cuando le convenga. Una reversion que no encaja en una
 * de estas cinco no es una reversion, y anadir un valor aqui sin cambiar
 * {@code chk_prr_causal_value} hace que el {@code INSERT} lo rechace la base y
 * el fallo llegue como un 409 sin explicacion.
 *
 * <ul>
 * <li>{@link #FRAUD} — fraude.
 * <li>{@link #UNSOLICITED_OPERATION} — operacion no solicitada.
 * <li>{@link #PRODUCT_NOT_RECEIVED} — producto no recibido.
 * <li>{@link #NOT_AS_ORDERED} — no corresponde a lo pedido.
 * <li>{@link #DEFECTIVE} — defectuoso.
 * </ul>
 */
public enum ReversalCausal {
    FRAUD, UNSOLICITED_OPERATION, PRODUCT_NOT_RECEIVED, NOT_AS_ORDERED, DEFECTIVE
}
