package com.vetsoftware.app.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyección del libro de compras (stock_movement type=PURCHASE ⋈ products ⋈
 * branches).
 */
public interface PurchaseRow {
    Long getId();

    Long getProductId();

    String getProductName();

    String getProductCode();

    Long getLotId();

    Long getBranchId();

    String getBranchName();

    int getQuantity();

    BigDecimal getUnitCost();

    Long getReferenceId();

    LocalDateTime getCreatedDate();
}
