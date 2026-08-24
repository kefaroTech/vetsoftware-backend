package com.vetsoftware.app.dunning.domain;

/**
 * Companion VO del contrato que vive en {@code subscription}. Copia local: este
 * slice no importa el dominio ajeno.
 *
 * <p>
 * {@code companyId} es obligatorio porque es la mitad de la clave compuesta
 * {@code (company_id, subscription_id)} con la que la base impide anotar un
 * evento de cobranza contra el contrato de otra clinica.
 */
public record SubscriptionRef(Long id, Long companyId, String subscriptionNumber, String status) {
    public SubscriptionRef {
        if (id == null)
            throw new IllegalArgumentException("subscription id is required");
        if (companyId == null)
            throw new IllegalArgumentException("subscription company id is required");
    }
}
