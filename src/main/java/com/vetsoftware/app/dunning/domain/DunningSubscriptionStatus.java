package com.vetsoftware.app.dunning.domain;

/** Copia local de los estados que el motor de cobranza necesita decidir. */
public enum DunningSubscriptionStatus {
    TRIALING, ACTIVE, PAST_DUE, READ_ONLY, CANCELLED, EXPIRED;

    public boolean isTerminal() {
        return this == CANCELLED || this == EXPIRED;
    }
}
