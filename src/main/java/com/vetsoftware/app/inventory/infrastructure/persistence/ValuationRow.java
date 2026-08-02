package com.vetsoftware.app.inventory.infrastructure.persistence;

import java.math.BigDecimal;

/**
 * Proyección de la valuación agregada por producto (Σ sobre stock_lot). Los SUM
 * llegan como BigDecimal.
 */
public interface ValuationRow {
    Long getProductId();

    String getProductName();

    String getProductCode();

    BigDecimal getQuantity();

    BigDecimal getValue();
}
