package com.vetsoftware.app.inventory.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lote de un producto en UNA sede: lleva el costo real de entrada ({@code unitCost}) y el vencimiento para FEFO.
 * El saldo disponible del lote ({@code quantityAvailable}) es la parte materializada del kardex. FKs por id plano
 * (company/branch/product) — el inventario no necesita datos de esos agregados, solo referenciarlos.
 */
public class StockLot {
    private Long id;
    private final Long companyId;
    private final Long branchId;
    private final Long productId;
    private final String lotNumber;      // nullable = lote genérico (sin número)
    private final LocalDate expireDate;  // nullable = no vence
    private int quantityAvailable;
    private final BigDecimal unitCost;
    private final LocalDateTime receivedDate;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public StockLot(Long id, Long companyId, Long branchId, Long productId, String lotNumber, LocalDate expireDate,
                    int quantityAvailable, BigDecimal unitCost, LocalDateTime receivedDate,
                    LocalDateTime createdDate, boolean enabled) {
        if (companyId == null) throw new IllegalArgumentException("companyId is required");
        if (branchId == null) throw new IllegalArgumentException("branchId is required");
        if (productId == null) throw new IllegalArgumentException("productId is required");
        if (unitCost == null || unitCost.signum() < 0) throw new IllegalArgumentException("unitCost must be >= 0");
        this.id = id;
        this.companyId = companyId;
        this.branchId = branchId;
        this.productId = productId;
        this.lotNumber = lotNumber;
        this.expireDate = expireDate;
        this.quantityAvailable = quantityAvailable;
        this.unitCost = unitCost;
        this.receivedDate = receivedDate;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static StockLot create(Long companyId, Long branchId, Long productId, String lotNumber,
                                  LocalDate expireDate, int quantity, BigDecimal unitCost) {
        LocalDateTime now = LocalDateTime.now();
        return new StockLot(null, companyId, branchId, productId, lotNumber, expireDate, quantity, unitCost,
            now, now, true);
    }

    /** Suma unidades al lote (entrada). */
    public void add(int quantity) {
        this.quantityAvailable += quantity;
    }

    /** Resta unidades del lote (salida). Puede dejar el lote en negativo si la empresa permite stock negativo. */
    public void consume(int quantity) {
        this.quantityAvailable -= quantity;
    }

    public Long getId() { return id; }
    public void assignId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public Long getBranchId() { return branchId; }
    public Long getProductId() { return productId; }
    public String getLotNumber() { return lotNumber; }
    public LocalDate getExpireDate() { return expireDate; }
    public int getQuantityAvailable() { return quantityAvailable; }
    public BigDecimal getUnitCost() { return unitCost; }
    public LocalDateTime getReceivedDate() { return receivedDate; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void disable() { this.enabled = false; }
}
