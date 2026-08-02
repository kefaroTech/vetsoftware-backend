package com.vetsoftware.app.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyección del kardex para exportar: movimiento + nombres de producto/sede
 * (join).
 */
public interface KardexRow {
    String getProductName();

    String getProductCode();

    String getBranchName();

    LocalDateTime getCreatedDate();

    String getType();

    String getReferenceType();

    Long getReferenceId();

    Long getLotId();

    int getQuantity();

    BigDecimal getUnitCost();
}
