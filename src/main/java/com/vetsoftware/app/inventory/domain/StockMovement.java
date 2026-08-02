package com.vetsoftware.app.inventory.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Asiento del kardex: append-only e inmutable (las correcciones son movimientos
 * nuevos, nunca UPDATE/DELETE). {@code quantity} lleva el signo según
 * {@link StockMovementType#isInbound()}: entradas +, salidas −.
 */
public class StockMovement {
    private Long id;
    private final Long companyId;
    private final Long branchId;
    private final Long productId;
    private final Long lotId;
    private final StockMovementType type;
    private final int quantity;
    private final BigDecimal unitCost;
    private final StockReferenceType referenceType;
    private final Long referenceId;
    private final String reason;
    private final Long createdBy;
    private final LocalDateTime createdDate;

    public StockMovement(Long id, Long companyId, Long branchId, Long productId, Long lotId,
            StockMovementType type, int quantity, BigDecimal unitCost,
            StockReferenceType referenceType, Long referenceId, String reason, Long createdBy,
            LocalDateTime createdDate) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (branchId == null)
            throw new IllegalArgumentException("branchId is required");
        if (productId == null)
            throw new IllegalArgumentException("productId is required");
        if (lotId == null)
            throw new IllegalArgumentException("lotId is required");
        if (type == null)
            throw new IllegalArgumentException("type is required");
        if (referenceType == null)
            throw new IllegalArgumentException("referenceType is required");
        this.id = id;
        this.companyId = companyId;
        this.branchId = branchId;
        this.productId = productId;
        this.lotId = lotId;
        this.type = type;
        this.quantity = quantity;
        this.unitCost = unitCost == null ? BigDecimal.ZERO : unitCost;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
    }

    /**
     * Crea un movimiento con la cantidad ya con el signo correcto según el tipo
     * (entrada +, salida −). {@code absQuantity} es siempre positiva; el tipo
     * decide el signo persistido.
     */
    public static StockMovement of(Long companyId, Long branchId, Long productId, Long lotId,
            StockMovementType type, int absQuantity, BigDecimal unitCost,
            StockReferenceType referenceType, Long referenceId, String reason, Long createdBy) {
        int signed = type.isInbound() ? Math.abs(absQuantity) : -Math.abs(absQuantity);
        return new StockMovement(null, companyId, branchId, productId, lotId, type, signed,
                unitCost, referenceType, referenceId, reason, createdBy, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getLotId() {
        return lotId;
    }

    public StockMovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public StockReferenceType getReferenceType() {
        return referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public String getReason() {
        return reason;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
