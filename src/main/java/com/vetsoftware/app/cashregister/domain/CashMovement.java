package com.vetsoftware.app.cashregister.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento de caja (entidad hija, append-only e inmutable). El {@code amount}
 * es siempre positivo; el signo lo da el {@link CashMovementType#isInflow()}.
 * Nunca se hace UPDATE/DELETE: las correcciones son movimientos nuevos
 * ({@code VOID_OUT} para reversar una venta/abono, {@code
 * MANUAL_*} para ajustes de operador).
 */
public class CashMovement {
    private Long id;
    private final CashMovementType type;
    private final CashPaymentMethod method;
    private final BigDecimal amount;
    private final CashReferenceType referenceType;
    private final Long referenceId;
    private final Long createdByEmployeeId;
    private final LocalDateTime createdAt;
    private final String note;

    public CashMovement(Long id, CashMovementType type, CashPaymentMethod method, BigDecimal amount,
            CashReferenceType referenceType, Long referenceId, Long createdByEmployeeId,
            LocalDateTime createdAt, String note) {
        if (type == null)
            throw new IllegalArgumentException("type is required");
        if (method == null)
            throw new IllegalArgumentException("method is required");
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("amount must be positive");
        if (referenceType == null)
            throw new IllegalArgumentException("referenceType is required");
        this.id = id;
        this.type = type;
        this.method = method;
        this.amount = amount;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdByEmployeeId = createdByEmployeeId;
        this.createdAt = createdAt;
        this.note = note;
    }

    public static CashMovement create(CashMovementType type, CashPaymentMethod method,
            BigDecimal amount, CashReferenceType referenceType, Long referenceId,
            Long createdByEmployeeId, String note) {
        return new CashMovement(null, type, method, amount, referenceType, referenceId,
                createdByEmployeeId, LocalDateTime.now(), note);
    }

    /**
     * Aporte con signo al total del método: {@code +amount} si entra,
     * {@code -amount} si sale.
     */
    public BigDecimal signedAmount() {
        return type.isInflow() ? amount : amount.negate();
    }

    public Long getId() {
        return id;
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public CashMovementType getType() {
        return type;
    }

    public CashPaymentMethod getMethod() {
        return method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public CashReferenceType getReferenceType() {
        return referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public Long getCreatedByEmployeeId() {
        return createdByEmployeeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getNote() {
        return note;
    }
}
