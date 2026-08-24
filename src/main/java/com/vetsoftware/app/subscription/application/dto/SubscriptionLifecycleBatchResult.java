package com.vetsoftware.app.subscription.application.dto;

/** Resultado de una pagina del barrido diario de contratos. */
public record SubscriptionLifecycleBatchResult(int processed, long lastId) {

    public SubscriptionLifecycleBatchResult {
        if (processed < 0)
            throw new IllegalArgumentException("processed must not be negative");
        if (lastId < 0)
            throw new IllegalArgumentException("lastId must not be negative");
    }
}
