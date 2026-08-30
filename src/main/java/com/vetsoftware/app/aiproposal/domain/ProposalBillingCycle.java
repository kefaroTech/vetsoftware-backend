package com.vetsoftware.app.aiproposal.domain;

/**
 * Ciclo de facturacion con el que se cotiza la propuesta
 * ({@code chk_ai_proposals_cycle}).
 *
 * <p>
 * <strong>Es un enum propio y no el de {@code pricelist}</strong>: el vertical
 * slicing prohibe que el dominio de una feature importe el de otra, y el precio
 * llega ya resuelto a esta rodaja dentro de {@link SellableItem}.
 */
public enum ProposalBillingCycle {

    MONTHLY,

    ANNUAL
}
