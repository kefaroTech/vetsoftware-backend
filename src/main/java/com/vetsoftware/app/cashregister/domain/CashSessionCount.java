package com.vetsoftware.app.cashregister.domain;

import java.math.BigDecimal;

/**
 * Conteo de cierre por medio de pago: lo que el sistema esperaba ({@code expectedAmount}) frente a lo que se declaró
 * contado ({@code countedAmount}); la {@code difference} (contado − esperado) es el sobrante (+) o faltante (−). Se
 * materializa solo al cerrar la sesión. Para no-efectivo suele cuadrar (conciliación por medio); solo el efectivo se
 * cuenta físicamente.
 */
public class CashSessionCount {
    private Long id;
    private final CashPaymentMethod method;
    private final BigDecimal expectedAmount;
    private final BigDecimal countedAmount;

    public CashSessionCount(Long id, CashPaymentMethod method, BigDecimal expectedAmount, BigDecimal countedAmount) {
        if (method == null) throw new IllegalArgumentException("method is required");
        if (expectedAmount == null) throw new IllegalArgumentException("expectedAmount is required");
        if (countedAmount == null) throw new IllegalArgumentException("countedAmount is required");
        this.id = id;
        this.method = method;
        this.expectedAmount = expectedAmount;
        this.countedAmount = countedAmount;
    }

    public static CashSessionCount create(CashPaymentMethod method, BigDecimal expectedAmount,
                                          BigDecimal countedAmount) {
        return new CashSessionCount(null, method, expectedAmount, countedAmount);
    }

    /** Diferencia de arqueo = contado − esperado. Positiva sobra, negativa falta, cero cuadra. */
    public BigDecimal difference() {
        return countedAmount.subtract(expectedAmount);
    }

    public Long getId() { return id; }
    public void assignId(Long id) { this.id = id; }
    public CashPaymentMethod getMethod() { return method; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public BigDecimal getCountedAmount() { return countedAmount; }
}
