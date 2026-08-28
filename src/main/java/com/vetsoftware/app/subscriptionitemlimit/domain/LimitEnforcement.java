package com.vetsoftware.app.subscriptionitemlimit.domain;

/** Qué hacer al llegar al tope, congelado el día de la firma. */
public enum LimitEnforcement {
    WARN, BLOCK, READ_ONLY, OVERAGE;

    public boolean requiresOveragePrice() {
        return this == OVERAGE;
    }

    public boolean allowsCreationOverLimit() {
        return this == WARN || this == OVERAGE;
    }
}
