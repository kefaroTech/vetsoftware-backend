package com.vetsoftware.app.dunning.domain;

/** Estado del contrato bajo bloqueo, separado del dominio de subscription. */
public record DunningSubscriptionSnapshot(SubscriptionRef subscription,
        DunningSubscriptionStatus status, int graceDays) {
    public DunningSubscriptionSnapshot {
        if (subscription == null)
            throw new IllegalArgumentException("subscription is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (graceDays < 0)
            throw new IllegalArgumentException("graceDays must not be negative");
    }
}
