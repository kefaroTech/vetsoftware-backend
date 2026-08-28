package com.vetsoftware.app.subscriptionitemlimit.domain;

/** Si la línea firmada trae techo o no. */
public enum LimitMode {
    FULL, LIMITED;

    public boolean requiresQuantity() {
        return this == LIMITED;
    }
}
