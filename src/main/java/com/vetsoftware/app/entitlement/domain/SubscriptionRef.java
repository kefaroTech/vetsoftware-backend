package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;

/**
 * Companion VO del contrato desde el que se derivan los permisos. Trae lo justo
 * para decidir: que contrato es, en que estado esta y hasta cuando dura la
 * prueba.
 */
public record SubscriptionRef(Long id, ContractStatus status, LocalDate trialEndDate) {
    public SubscriptionRef {
        if (id == null)
            throw new IllegalArgumentException("subscription id is required");
        if (status == null)
            throw new IllegalArgumentException("subscription status is required");
        // Espejo de chk_subscriptions_trial: un contrato en prueba sin fecha de fin
        // de prueba es una prueba que no caduca nunca.
        if (status == ContractStatus.TRIALING && trialEndDate == null)
            throw new IllegalArgumentException("trial end date is required while TRIALING");
    }
}
