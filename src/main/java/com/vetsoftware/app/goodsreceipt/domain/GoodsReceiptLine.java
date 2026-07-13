package com.vetsoftware.app.goodsreceipt.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Línea de una recepción de mercancía: qué producto entró, en qué lote/vencimiento, cuánto y a qué costo.
 * {@code purchaseOrderLineId} enlaza (opcionalmente) a la línea de la orden de compra para recepción parcial.
 */
public class GoodsReceiptLine {
    private final Long id;
    private final ProductRef product;
    private final Long purchaseOrderLineId;
    private final String lotNumber;
    private final LocalDate expireDate;
    private final int quantityReceived;
    private final BigDecimal unitCost;

    public GoodsReceiptLine(Long id, ProductRef product, Long purchaseOrderLineId, String lotNumber,
                            LocalDate expireDate, int quantityReceived, BigDecimal unitCost) {
        validate(product, lotNumber, quantityReceived, unitCost);
        this.id = id;
        this.product = product;
        this.purchaseOrderLineId = purchaseOrderLineId;
        this.lotNumber = lotNumber;
        this.expireDate = expireDate;
        this.quantityReceived = quantityReceived;
        this.unitCost = unitCost;
    }

    public static GoodsReceiptLine create(ProductRef product, Long purchaseOrderLineId, String lotNumber,
                                          LocalDate expireDate, int quantityReceived, BigDecimal unitCost) {
        return new GoodsReceiptLine(null, product, purchaseOrderLineId, lotNumber, expireDate,
            quantityReceived, unitCost);
    }

    private static void validate(ProductRef product, String lotNumber, int quantityReceived, BigDecimal unitCost) {
        if (product == null) throw new IllegalArgumentException("line product is required");
        if (lotNumber != null && lotNumber.length() > 60) throw new IllegalArgumentException("lotNumber must be 60 chars or less");
        if (quantityReceived <= 0) throw new IllegalArgumentException("quantityReceived must be greater than 0");
        if (unitCost == null) throw new IllegalArgumentException("unitCost is required");
        if (unitCost.signum() < 0) throw new IllegalArgumentException("unitCost cannot be negative");
    }

    public Long getId() { return id; }
    public ProductRef getProduct() { return product; }
    public Long getPurchaseOrderLineId() { return purchaseOrderLineId; }
    public String getLotNumber() { return lotNumber; }
    public LocalDate getExpireDate() { return expireDate; }
    public int getQuantityReceived() { return quantityReceived; }
    public BigDecimal getUnitCost() { return unitCost; }
}
