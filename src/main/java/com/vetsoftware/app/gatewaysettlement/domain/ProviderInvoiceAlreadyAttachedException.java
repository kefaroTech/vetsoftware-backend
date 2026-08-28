package com.vetsoftware.app.gatewaysettlement.domain;

/**
 * El lote ya tiene escrito el soporte del gasto y no se sobrescribe.
 *
 * <p>
 * El mensaje lleva <strong>la referencia que ya estaba</strong> a proposito:
 * quien intenta escribir la segunda necesita saber cual es la primera para
 * decidir si se equivoco de lote o si de verdad llegaron dos facturas — y en
 * ese caso lo que cambio fue el gasto, que es otra fila.
 */
public class ProviderInvoiceAlreadyAttachedException extends RuntimeException {

    public ProviderInvoiceAlreadyAttachedException(Long id, String providerInvoiceRef) {
        super("Gateway settlement " + id + " already has provider invoice " + providerInvoiceRef);
    }
}
