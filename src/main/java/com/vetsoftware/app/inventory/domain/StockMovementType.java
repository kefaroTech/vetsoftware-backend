package com.vetsoftware.app.inventory.domain;

/**
 * Tipo de movimiento del kardex. {@code inbound} indica si suma stock (+) o lo
 * resta (−); el signo de {@code quantity} en {@code StockMovement} sigue esta
 * convención.
 */
public enum StockMovementType {
    PURCHASE(true), SALE(false), ADJUSTMENT_IN(true), ADJUSTMENT_OUT(false), CLINICAL_USE(
            false), TRANSFER_OUT(false), TRANSFER_IN(true), VOID_IN(true), VOID_OUT(false);

    private final boolean inbound;

    StockMovementType(boolean inbound) {
        this.inbound = inbound;
    }

    public boolean isInbound() {
        return inbound;
    }
}
