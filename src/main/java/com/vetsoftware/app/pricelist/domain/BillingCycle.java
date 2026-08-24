package com.vetsoftware.app.pricelist.domain;

/**
 * Periodicidad con la que se cobra un precio del catálogo.
 *
 * <p>
 * El precio anual lleva <strong>su propio importe</strong>, no un descuento
 * calculado sobre el mensual: así el descuento anual es un dato auditable y no
 * una fórmula escondida en el código.
 */
public enum BillingCycle {
    MONTHLY, ANNUAL
}
