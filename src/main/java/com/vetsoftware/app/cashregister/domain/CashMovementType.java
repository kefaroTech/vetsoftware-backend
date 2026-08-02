package com.vetsoftware.app.cashregister.domain;

/**
 * Tipo de movimiento de caja. {@code inflow} indica si suma dinero (+) o lo
 * resta (−); el {@code
 * amount} del {@link CashMovement} siempre es positivo y su signo lo da este
 * tipo (igual que {@code
 * StockMovementType.isInbound()}). Los tipos
 * {@link #SALE_IN}/{@link #OPEN_ACCOUNT_IN}/{@link #VOID_OUT} solo los inyecta
 * la orquestación (POS / cuenta abierta); el REST manual acepta únicamente
 * {@link #MANUAL_IN}/{@link #WITHDRAWAL}/{@link #EXPENSE}.
 */
public enum CashMovementType {
    SALE_IN(true), OPEN_ACCOUNT_IN(true), MANUAL_IN(true), WITHDRAWAL(false), EXPENSE(
            false), VOID_OUT(false);

    private final boolean inflow;

    CashMovementType(boolean inflow) {
        this.inflow = inflow;
    }

    /** ¿El movimiento entra dinero a la caja (+) o lo saca (−)? */
    public boolean isInflow() {
        return inflow;
    }

    /**
     * Tipos que un operador puede registrar a mano desde el REST (los demás los
     * inyecta la orquestación).
     */
    public boolean isManual() {
        return this == MANUAL_IN || this == WITHDRAWAL || this == EXPENSE;
    }
}
