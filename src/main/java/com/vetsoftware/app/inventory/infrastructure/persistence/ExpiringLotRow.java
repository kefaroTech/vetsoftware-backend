package com.vetsoftware.app.inventory.infrastructure.persistence;

import java.time.LocalDate;

/**
 * Proyección de la búsqueda de lotes por vencer (stock_lot ⋈ products ⋈
 * branches).
 */
public interface ExpiringLotRow {
    Long getLotId();

    Long getProductId();

    String getProductName();

    String getProductCode();

    Long getBranchId();

    String getBranchName();

    String getLotNumber();

    LocalDate getExpireDate();

    int getQuantityAvailable();

    long getDaysToExpire();
}
